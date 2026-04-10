package com.mochu.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情/列表 VO — 对照 V3.2 P.1（排除 password_hash, deleted）
 */
@Data
public class UserVO {

    private Integer id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private Integer deptId;

    /** 部门名称（关联查询） */
    private String deptName;

    private String position;

    private String avatar;

    private Integer status;

    private Integer flagContact;

    private Integer privacyMode;

    private Integer loginAttempts;

    private LocalDateTime lastLoginTime;

    private LocalDateTime lockUntil;

    private Integer forceChangePwd;

    private String wxUserid;

    private Integer creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 用户当前角色ID列表 */
    private List<Integer> roleIds;

    /** 用户角色编码列表 */
    private List<String> roleCodes;

    /** 用户权限编码列表 */
    private List<String> permissions;
}
