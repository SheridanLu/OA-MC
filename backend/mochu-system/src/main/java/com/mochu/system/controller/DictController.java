package com.mochu.system.controller;

import com.mochu.common.result.PageResult;
import com.mochu.common.result.R;
import com.mochu.system.dto.DictDataDTO;
import com.mochu.system.dto.DictDataQueryDTO;
import com.mochu.system.dto.DictTypeDTO;
import com.mochu.system.dto.DictTypeQueryDTO;
import com.mochu.system.service.DictService;
import com.mochu.system.vo.DictDataVO;
import com.mochu.system.vo.DictTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== DictType ====================

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<PageResult<DictTypeVO>> listDictTypes(DictTypeQueryDTO query) {
        return R.ok(dictService.listDictTypes(query));
    }

    @GetMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<DictTypeVO> getDictTypeById(@PathVariable Integer id) {
        return R.ok(dictService.getDictTypeById(id));
    }

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> createDictType(@RequestBody DictTypeDTO dto) {
        dictService.createDictType(dto);
        return R.ok();
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> updateDictType(@PathVariable Integer id, @RequestBody DictTypeDTO dto) {
        dictService.updateDictType(id, dto);
        return R.ok();
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> deleteDictType(@PathVariable Integer id) {
        dictService.deleteDictType(id);
        return R.ok();
    }

    // ==================== DictData ====================

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<PageResult<DictDataVO>> listDictData(DictDataQueryDTO query) {
        return R.ok(dictService.listDictData(query));
    }

    @GetMapping("/data/type/{dictType}")
    public R<List<DictDataVO>> getDictDataByType(@PathVariable String dictType) {
        return R.ok(dictService.getDictDataByType(dictType));
    }

    @PostMapping("/data")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> createDictData(@RequestBody DictDataDTO dto) {
        dictService.createDictData(dto);
        return R.ok();
    }

    @PutMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> updateDictData(@PathVariable Integer id, @RequestBody DictDataDTO dto) {
        dictService.updateDictData(id, dto);
        return R.ok();
    }

    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict-manage')")
    public R<Void> deleteDictData(@PathVariable Integer id) {
        dictService.deleteDictData(id);
        return R.ok();
    }
}
