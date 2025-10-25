package com.web.service;

import com.web.mapper.RolePermissionMapper;
import com.web.model.Permission;
import com.web.model.Role;
import com.web.model.RolePermission;
import com.web.service.Impl.RolePermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RolePermissionService测试类
 * 验证角色权限管理功能的正确性
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色权限服务测试")
public class RolePermissionServiceTest {

    @Mock
    private RolePermissionMapper rolePermissionMapper;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    private Role testRole;
    private Permission testPermission;
    private RolePermission testRolePermission;

    @BeforeEach
    void setUp() {
        // 创建测试角色
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRoleName("测试角色");
        testRole.setRoleCode("TEST_ROLE");
        testRole.setDescription("用于测试的角色");

        // 创建测试权限
        testPermission = new Permission();
        testPermission.setId(1L);
        testPermission.setName("测试权限");
        testPermission.setCode("TEST_PERMISSION");
        testPermission.setDescription("用于测试的权限");

        // 创建测试角色权限关联
        testRolePermission = new RolePermission();
        testRolePermission.setId(1L);
        testRolePermission.setRoleId(testRole.getId());
        testRolePermission.setPermissionId(testPermission.getId());
        testRolePermission.setPermission(testPermission);
        testRolePermission.setStatus(1);
        testRolePermission.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        testRolePermission.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
    }

    @Test
    @DisplayName("分配权限给角色")
    void testAssignPermissionToRole() {
        // Given
        Long roleId = testRole.getId();
        Long permissionId = testPermission.getId();
        Long operatorId = 1L;

        when(rolePermissionMapper.existsByRoleIdAndPermissionId(roleId, permissionId)).thenReturn(false);
        when(rolePermissionMapper.insert(any(RolePermission.class))).thenReturn(1);

        // When
        boolean result = rolePermissionService.assignPermissionToRole(roleId, permissionId, operatorId);

        // Then
        assertTrue(result, "权限分配应该成功");
        verify(rolePermissionMapper).existsByRoleIdAndPermissionId(roleId, permissionId);
        verify(rolePermissionMapper).insert(any(RolePermission.class));
    }

    @Test
    @DisplayName("分配已存在的权限应该失败")
    void testAssignExistingPermissionShouldFail() {
        // Given
        Long roleId = testRole.getId();
        Long permissionId = testPermission.getId();
        Long operatorId = 1L;

        when(rolePermissionMapper.existsByRoleIdAndPermissionId(roleId, permissionId)).thenReturn(true);

        // When
        boolean result = rolePermissionService.assignPermissionToRole(roleId, permissionId, operatorId);

        // Then
        assertFalse(result, "已存在的权限分配应该失败");
        verify(rolePermissionMapper).existsByRoleIdAndPermissionId(roleId, permissionId);
        verify(rolePermissionMapper, never()).insert(any(RolePermission.class));
    }

    @Test
    @DisplayName("移除角色权限")
    void testRemovePermissionFromRole() {
        // Given
        Long roleId = testRole.getId();
        Long permissionId = testPermission.getId();

        when(rolePermissionMapper.deleteByRoleIdAndPermissionId(roleId, permissionId)).thenReturn(1);

        // When
        boolean result = rolePermissionService.removePermissionFromRole(roleId, permissionId);

        // Then
        assertTrue(result, "权限移除应该成功");
        verify(rolePermissionMapper).deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Test
    @DisplayName("获取角色权限列表")
    void testGetRolePermissions() {
        // Given
        Long roleId = testRole.getId();
        List<RolePermission> rolePermissions = Arrays.asList(testRolePermission);

        when(rolePermissionMapper.findByRoleId(roleId)).thenReturn(rolePermissions);

        // When
        List<Permission> permissions = rolePermissionService.getRolePermissions(roleId);

        // Then
        assertNotNull(permissions, "权限列表不应为空");
        assertEquals(1, permissions.size(), "应该返回1个权限");
        assertEquals(testPermission.getId(), permissions.get(0).getId(), "权限ID应该匹配");
        assertEquals(testPermission.getName(), permissions.get(0).getName(), "权限名称应该匹配");
        verify(rolePermissionMapper).findByRoleId(roleId);
    }

    @Test
    @DisplayName("批量分配权限给角色")
    void testBatchAssignPermissionsToRole() {
        // Given
        Long roleId = testRole.getId();
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        Long operatorId = 1L;

        when(rolePermissionMapper.existsByRoleIdAndPermissionId(roleId, 1L)).thenReturn(false);
        when(rolePermissionMapper.existsByRoleIdAndPermissionId(roleId, 2L)).thenReturn(false);
        when(rolePermissionMapper.existsByRoleIdAndPermissionId(roleId, 3L)).thenReturn(false);
        when(rolePermissionMapper.batchInsert(anyList())).thenReturn(3);

        // When
        Map<String, Object> result = rolePermissionService.batchAssignPermissionsToRole(roleId, permissionIds, operatorId);

        // Then
        assertNotNull(result, "结果不应为空");
        assertEquals(3, result.get("successCount"), "成功数量应该为3");
        assertEquals(0, result.get("failCount"), "失败数量应该为0");
        verify(rolePermissionMapper, times(3)).existsByRoleIdAndPermissionId(anyLong(), anyLong());
        verify(rolePermissionMapper).batchInsert(anyList());
    }

    @Test
    @DisplayName("检查角色权限")
    void testHasPermission() {
        // Given
        Long roleId = testRole.getId();
        String permissionCode = testPermission.getCode();

        when(rolePermissionMapper.countByRoleIdAndPermissionCode(roleId, permissionCode)).thenReturn(1L);

        // When
        boolean result = rolePermissionService.hasPermission(roleId, permissionCode);

        // Then
        assertTrue(result, "角色应该拥有指定权限");
        verify(rolePermissionMapper).countByRoleIdAndPermissionCode(roleId, permissionCode);
    }

    @Test
    @DisplayName("获取角色权限统计")
    void testGetRolePermissionStats() {
        // Given
        Long roleId = testRole.getId();

        when(rolePermissionMapper.countByRoleId(roleId)).thenReturn(5L);

        // When
        Map<String, Object> stats = rolePermissionService.getRolePermissionStats(roleId);

        // Then
        assertNotNull(stats, "统计信息不应为空");
        assertEquals(5L, stats.get("totalPermissions"), "总权限数应该为5");
        verify(rolePermissionMapper).countByRoleId(roleId);
    }
}