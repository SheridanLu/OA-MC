package com.mochu.common.security;

import com.mochu.common.exception.BusinessException;
import java.util.Arrays;
import java.util.Set;

/**
 * 角色直控校验工具
 */
public class RoleCheckUtils {

    /**
     * 要求用户拥有指定角色之一
     * @param loginUser 当前用户
     * @param roles     允许的角色编码
     * @throws BusinessException 如果无权限
     */
    public static void requireAnyRole(LoginUser loginUser, String... roles) {
        if (loginUser == null || loginUser.getPermissions() == null) {
            throw new BusinessException(403, "无权限访问");
        }
        Set<String> userPermissions = loginUser.getPermissions();
        boolean hasRole = Arrays.stream(roles).anyMatch(userPermissions::contains);
        if (!hasRole) {
            throw new BusinessException(403,
                    "仅" + String.join("/", roles) + "角色可操作");
        }
    }
}
