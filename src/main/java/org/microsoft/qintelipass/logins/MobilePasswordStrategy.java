package org.microsoft.qintelipass.logins;

import org.microsoft.qintelipass.ILoginStrategy;
import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.response.ResponseBody;

import java.util.Map;

public class MobilePasswordStrategy implements ILoginStrategy {
    @Override
    public String getType() {
        return "MOBILE_PWD";
    }
    @Override
    public ResponseBody<User> authenticate(Map<String, Object> params) {
        return null;
    }
}
