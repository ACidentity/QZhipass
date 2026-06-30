package org.microsoft.qintelipass.controllers;

import org.microsoft.qintelipass.models.User;
import org.microsoft.qintelipass.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class UserController {
    @Autowired
    private UserService userService;
    
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        List<User> allUsers = userService.getAllUsers();
        
        // 搜索过滤
        List<User> filteredUsers = allUsers;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            filteredUsers = allUsers.stream()
                .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(lowerKeyword))
                        || (u.getPhone() != null && u.getPhone().contains(keyword)))
                .collect(Collectors.toList());
        }
        
        // 分页
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, filteredUsers.size());
        
        if (startIndex >= filteredUsers.size()) {
            startIndex = 0;
            endIndex = Math.min(size, filteredUsers.size());
        }
        
        List<User> pageUsers = filteredUsers.subList(startIndex, endIndex);
        
        // 返回格式：{ total: 100, items: [...] }
        Map<String, Object> response = new HashMap<>();
        response.put("total", filteredUsers.size());
        response.put("items", pageUsers);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deactivateUser(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid user ID"));
        }
        
        boolean success = userService.deactivateUser(userId);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User deactivated successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to deactivate user. User may not exist or already be deactivated."));
    }
}
