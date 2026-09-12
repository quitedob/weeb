package com.web.service.impl;

import com.web.constant.UserType;
import com.web.mapper.AuthMapper;
import com.web.model.User;
import com.web.service.UserTypeSecurityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserTypeSecurityServiceImpl implements UserTypeSecurityService {
    private final AuthMapper authMapper;

    public UserTypeSecurityServiceImpl(AuthMapper authMapper) { this.authMapper = authMapper; }

    @Override
    public String getUserType(String username) {
        if (username == null || username.isBlank()) return "UNKNOWN";
        User user = authMapper.findByUsername(username.trim());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) return "UNKNOWN";
        return UserType.normalize(user.getType());
    }

    @Override public boolean isAdmin(String username) { return UserType.ADMIN.equals(getUserType(username)); }
    @Override public boolean isBot(String username) { return UserType.BOT.equals(getUserType(username)); }
    @Override public boolean isRegularUser(String username) { return UserType.USER.equals(getUserType(username)); }
    @Override public boolean canAccessAdminFeatures(String username) { return isAdmin(username); }
    @Override public boolean canWrite(String username) {
        String type = getUserType(username);
        return UserType.ADMIN.equals(type) || UserType.USER.equals(type);
    }
    @Override public boolean canRead(String username) { return !"UNKNOWN".equals(getUserType(username)); }
}
