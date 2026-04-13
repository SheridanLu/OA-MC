package com.mochu.system.controller;

import com.mochu.common.result.R;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.service.HomeService;
import com.mochu.system.service.UserService;
import com.mochu.system.vo.HomeVO;
import com.mochu.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 首页 + 个人信息接口 — 对照 V3.2 §4.2, §5.9.2
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;
    private final UserService userService;

    /**
     * 首页数据 — GET /api/v1/home
     */
    @GetMapping("/home")
    @PreAuthorize("isAuthenticated()")
    public R<HomeVO> home() {
        return R.ok(homeService.getHomeData());
    }

    /**
     * 待办数量 — GET /api/v1/home/todo-count — V3.2 §5.9.2
     */
    @GetMapping("/home/todo-count")
    @PreAuthorize("isAuthenticated()")
    public R<Integer> todoCount() {
        return R.ok(homeService.getTodoCount());
    }

    /**
     * 待办列表 — GET /api/v1/home/todo-list — V3.2 §5.9.2
     */
    @GetMapping("/home/todo-list")
    @PreAuthorize("isAuthenticated()")
    public R<List<Map<String, Object>>> todoList() {
        return R.ok(homeService.getTodoList());
    }

    /**
     * 当前登录用户信息 — GET /api/v1/user/me
     */
    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public R<UserVO> currentUser() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return R.ok(userService.getUserById(userId));
    }
}
