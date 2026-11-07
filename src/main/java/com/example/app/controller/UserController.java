package com.example.app.controller;

import com.example.app.model.EditUserInfoReq;
import com.example.app.common.models.RespMsg;
import com.example.app.model.UserProfileResp;
import com.example.app.security.CustomUserDetails;
import com.example.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "2. User", description = "User management APIs")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get a list of all users and their basic info."
            , description = """
            - Project owner can view all users and then invite users to projects (by id, ADMIN excluded).
            - ADMIN can check deleted users.
            - View all users including ADMIN account, normal USERs cannot access ADMIN profile.
            """)
    public List<UserProfileResp> getAllUsers(
            @RequestParam(value="isDeleted", required = false) String isDeleted,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getAllUsers(isDeleted, userDetails.getUser().getRole());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an user profile by user id",
            description = """
                    Logged in user can view user profile by user id. (ADMIN excluded)
                    - normal USERs cannot access ADMIN profile
                    """)
    public UserProfileResp getUserProfile(@PathVariable("id") Long userId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.getUserProfile(userId, userDetails.getUser().getRole());
    }

    @DeleteMapping("/{id}")
    @Tag(name = "ADMIN", description = """
            - deleted users
            - view all projects list, view a project detail by project ID
            - view all users including deleted users and ADMIN account, normal USERs cannot access ADMIN profile
            """)
    @Operation(summary = "ADMIN Delete user",
            description = """
            Delete a user by setting isDeleted to true.
            - ADMIN cannot be deleted.
            - Deleted user cannot login, and will not be shown in user list, project member list, \
            cannot be invited to projects and tasks
            """)
    public RespMsg deleteUser(@PathVariable("id") Long userId) {
        return userService.deleteUser(userId);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change user role, or edit user name and email",
            description = """
                    - ADMIN account is permitted to change other users' role from USER to ADMIN.
                    - Cannot change ADMIN role.
                    - Logged in user can edit their own user name and email
                    """)
    public RespMsg editUserRole(@PathVariable("id") Long id,
                                    @Valid @RequestBody EditUserInfoReq req,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return userService.editUser(id, req, userDetails.getId(), userDetails.getUser().getRole());
    }

}
