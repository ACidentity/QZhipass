package org.microsoft.qintelipass.services;

import org.microsoft.qintelipass.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService extends AbstractUserService {
    private final PasswordEncoder passwordEncoder;
    @Autowired
    public LoginService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    @Override
    public User login(String username, String password){
        User user = findByUsername(username);

        if (user != null) {
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }
}
