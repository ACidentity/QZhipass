package org.microsoft.qintelipass.models;

import org.microsoft.qintelipass.enums.UserStatus;

public class User {
    private String id;
    private String phone;
    private String wechatOpenId;
    private UserStatus status;
    private String name;
    
    public User() {}
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getWechatOpenId() { return wechatOpenId; }
    public void setWechatOpenId(String wechatOpenId) { this.wechatOpenId = wechatOpenId; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
