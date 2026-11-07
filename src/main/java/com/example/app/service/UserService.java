package com.example.app.service;

import com.example.app.model.EditUserInfoReq;
import com.example.app.common.models.RespMsg;
import com.example.app.model.UserProfileResp;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {

    /** ADMIN delete users (set is deleted to true, 停用) */
    RespMsg deleteUser(Long userId);

    UserProfileResp getUserProfile(Long id, String userRole);

    List<UserProfileResp> getAllUsers(String isDeleted, String userRole);

    RespMsg editUser(Long userId, EditUserInfoReq req, Long loginUserId, String userRole);
}
