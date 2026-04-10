package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.system.dto.UserCreateDTO;
import com.mochu.system.dto.UserQueryDTO;
import com.mochu.system.dto.UserUpdateDTO;
import com.mochu.system.entity.SysDept;
import com.mochu.system.entity.SysUser;
import com.mochu.system.entity.SysUserRole;
import com.mochu.system.mapper.SysDeptMapper;
import com.mochu.system.mapper.SysRoleMapper;
import com.mochu.system.mapper.SysUserMapper;
import com.mochu.system.mapper.SysUserRoleMapper;
import com.mochu.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务 — 对照 V3.2 §5.9.1
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户分页查询
     */
    public PageResult<UserVO> listUsers(UserQueryDTO dto) {
        int page = (dto.getPage() != null && dto.getPage() > 0) ? dto.getPage() : Constants.DEFAULT_PAGE;
        int size = (dto.getSize() != null && dto.getSize() > 0) ? dto.getSize() : Constants.DEFAULT_SIZE;

        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            wrapper.like(SysUser::getUsername, dto.getUsername());
        }
        if (dto.getRealName() != null && !dto.getRealName().isBlank()) {
            wrapper.like(SysUser::getRealName, dto.getRealName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()) {
            wrapper.like(SysUser::getPhone, dto.getPhone());
        }
        if (dto.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, dto.getDeptId());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(SysUser::getId);

        sysUserMapper.selectPage(pageParam, wrapper);

        List<UserVO> voList = pageParam.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(voList, pageParam.getTotal(), page, size);
    }

    /**
     * 用户详情
     */
    public UserVO getUserById(Integer id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    /**
     * 创建用户
     */
    @Transactional
    public Integer createUser(UserCreateDTO dto) {
        // 检查用户名唯一
        Long usernameCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号唯一
        Long phoneCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone())
        );
        if (phoneCount > 0) {
            throw new BusinessException("手机号已存在");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        if (user.getStatus() == null) user.setStatus(1);
        if (user.getFlagContact() == null) user.setFlagContact(1);
        if (user.getPrivacyMode() == null) user.setPrivacyMode(0);
        if (user.getForceChangePwd() == null) user.setForceChangePwd(0);
        user.setLoginAttempts(0);

        sysUserMapper.insert(user);
        return user.getId();
    }

    /**
     * 更新用户
     */
    @Transactional
    public void updateUser(UserUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查手机号唯一（如果修改了手机号）
        if (dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())) {
            Long phoneCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getPhone, dto.getPhone())
                            .ne(SysUser::getId, dto.getId())
            );
            if (phoneCount > 0) {
                throw new BusinessException("手机号已存在");
            }
        }

        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getDeptId() != null) user.setDeptId(dto.getDeptId());
        if (dto.getPosition() != null) user.setPosition(dto.getPosition());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (dto.getFlagContact() != null) user.setFlagContact(dto.getFlagContact());
        if (dto.getPrivacyMode() != null) user.setPrivacyMode(dto.getPrivacyMode());
        if (dto.getForceChangePwd() != null) user.setForceChangePwd(dto.getForceChangePwd());
        if (dto.getWxUserid() != null) user.setWxUserid(dto.getWxUserid());

        sysUserMapper.updateById(user);
    }

    /**
     * 删除用户（逻辑删除）
     */
    public void deleteUser(Integer id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        sysUserMapper.deleteById(id);
    }

    /**
     * 启用/禁用用户 — V3.2 §5.9.4 PATCH status
     */
    public void updateUserStatus(Integer id, Integer status) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        sysUserMapper.updateById(user);
    }

    /**
     * 分配用户角色
     */
    @Transactional
    public void assignRoles(Integer userId, List<Integer> roleIds) {
        // 清除原有角色
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        );
        // 保存新角色
        if (roleIds != null) {
            for (Integer roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insertUserRole(ur);
            }
        }
    }

    /**
     * 重置用户密码（管理员操作）
     */
    public void resetUserPassword(Integer userId, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForceChangePwd(1);
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        sysUserMapper.updateById(user);
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 查询部门名称
        if (user.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(user.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
        // 查询用户角色ID列表
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
        vo.setRoleIds(userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList()));
        // 查询用户角色编码列表
        vo.setRoleCodes(sysRoleMapper.selectRoleCodesByUserId(user.getId()));
        // 查询用户权限编码列表
        Set<String> permCodes = sysRoleMapper.selectPermCodesByUserId(user.getId());
        vo.setPermissions(new ArrayList<>(permCodes));
        return vo;
    }
}
