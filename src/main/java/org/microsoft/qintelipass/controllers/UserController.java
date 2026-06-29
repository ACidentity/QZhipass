package org.microsoft.qintelipass.controllers;

import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {
    @Autowired
    private UserService userService;
    
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        if (!isValidAdminKey(adminKey)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden: Admin access required"));
        }
        
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @PostMapping("/users/{userId}/deactivate")
    public ResponseEntity<?> deactivateUser(
            @PathVariable String userId,
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        
        if (!isValidAdminKey(adminKey)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden: Admin access required"));
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid user ID"));
        }
        
        boolean success = userService.deactivateUser(userId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User deactivated successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to deactivate user. User may not exist or already be deactivated."));
    }
    
    private boolean isValidAdminKey(String adminKey) {
        String expectedKey = System.getenv("ADMIN_SECRET_KEY");
        if (expectedKey == null || expectedKey.isEmpty()) {
            expectedKey = "admin-secret-key";
        }
        return expectedKey.equals(adminKey);
    }
}
