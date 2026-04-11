package com.mochu.infra.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.infra.dto.CodegenColumnDTO;
import com.mochu.infra.dto.CodegenTableDTO;
import com.mochu.infra.dto.CodegenTableQueryDTO;
import com.mochu.infra.service.CodegenService;
import com.mochu.infra.vo.CodegenColumnVO;
import com.mochu.infra.vo.CodegenTableVO;
import com.mochu.infra.vo.DbTableVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/infra/codegen")
@RequiredArgsConstructor
public class CodegenController {

    private final CodegenService codegenService;

    // ==================== 表管理 ====================

    @GetMapping("/tables")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<PageResult<CodegenTableVO>> listTables(CodegenTableQueryDTO query) {
        return R.ok(codegenService.listTables(query));
    }

    @GetMapping("/db-tables")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<List<DbTableVO>> listDbTables() {
        return R.ok(codegenService.listDbTables());
    }

    @PostMapping("/tables/import")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<Void> importTable(@RequestParam String tableName,
                               @RequestParam(required = false) String author) {
        codegenService.importTable(tableName, author);
        return R.ok();
    }

    @GetMapping("/tables/{id}")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<CodegenTableVO> getTableDetail(@PathVariable Integer id) {
        return R.ok(codegenService.getTableDetail(id));
    }

    @PutMapping("/tables/{id}")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<Void> updateTable(@PathVariable Integer id, @RequestBody CodegenTableDTO dto) {
        codegenService.updateTable(id, dto);
        return R.ok();
    }

    @DeleteMapping("/tables/{id}")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<Void> deleteTable(@PathVariable Integer id) {
        codegenService.deleteTable(id);
        return R.ok();
    }

    // ==================== 列管理 ====================

    @GetMapping("/tables/{id}/columns")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<List<CodegenColumnVO>> listColumns(@PathVariable Integer id) {
        return R.ok(codegenService.listColumnsVO(id));
    }

    @PutMapping("/tables/{id}/columns")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<Void> updateColumns(@PathVariable Integer id,
                                 @RequestBody List<CodegenColumnDTO> dtos) {
        codegenService.updateColumns(id, dtos);
        return R.ok();
    }

    // ==================== 代码生成 ====================

    @GetMapping("/tables/{id}/preview")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public R<Map<String, String>> previewCode(@PathVariable Integer id) {
        return R.ok(codegenService.previewCode(id));
    }

    @GetMapping("/tables/{id}/download")
    @PreAuthorize("hasAuthority('infra:codegen')")
    public void downloadCode(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        byte[] zip = codegenService.downloadCode(id);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=codegen.zip");
        response.setContentLength(zip.length);
        response.getOutputStream().write(zip);
    }
}
