package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.framework.annotation.Idempotent;
import com.mochu.system.dto.ConfigDTO;
import com.mochu.system.dto.ConfigQueryDTO;
import com.mochu.system.entity.SysConfig;
import com.mochu.system.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置接口
 */
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:config')")
    public R<PageResult<SysConfig>> list(ConfigQueryDTO query) {
        return R.ok(configService.list(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config')")
    public R<SysConfig> getById(@PathVariable Integer id) {
        SysConfig config = configService.getById(id);
        if (config == null) {
            return R.fail(404, "配置不存在");
        }
        return R.ok(config);
    }

    @Idempotent
    @PostMapping
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<Void> create(@Valid @RequestBody ConfigDTO dto) {
        configService.create(dto);
        return R.ok();
    }

    @Idempotent
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<Void> update(@PathVariable Integer id, @Valid @RequestBody ConfigDTO dto) {
        configService.update(id, dto);
        return R.ok();
    }

    @Idempotent
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public R<Void> delete(@PathVariable Integer id) {
        configService.delete(id);
        return R.ok();
    }
}
