package org.microsoft.qintelipass.logins;

import lombok.extern.slf4j.Slf4j;
import org.microsoft.qintelipass.ILoginStrategy;
import org.microsoft.qintelipass.enums.UserStatus;
import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.response.ResponseBody;
import org.microsoft.qintelipass.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PasswordLoginStrategy implements ILoginStrategy {

    @Autowired
    private UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String getType() {
        return "password";
    }

    @Override
    public ResponseBody authenticate(Map<String, Object> params) {
        String phone = (String) params.get("phone");
        String password = (String) params.get("password");

        if (phone == null || password == null) {
            return new ResponseBody(false, "Phone number and password are required.");
        }

        User user = userService.getUserByPhone(phone);
        if (user == null) {
            return new ResponseBody(false, "User not found.");
        }

        // Check account status
        if (UserStatus.CANCELLED.equals(user.getStatus())) {
            return new ResponseBody(false, "Your account has been cancelled.");
        }
        if (UserStatus.FROZEN.equals(user.getStatus())) {
            return new ResponseBody(false, "Your account has been frozen.");
        }

        // BCrypt password verification
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isEmpty()) {
            return new ResponseBody(false, "Password not set. Please use SMS login.");
        }

        if (!encoder.matches(password, storedPassword)) {
            return new ResponseBody(false, "Invalid password.");
        }

        // Login success
        ResponseBody response = new ResponseBody(true, "Login Successful.");
        response.setData(Map.of(
            "id", String.valueOf(user.getId()),
            "name", user.getName(),
            "phone", user.getPhone(),
            "status", user.getStatus().name()
        ));
        return response;
    }
}
