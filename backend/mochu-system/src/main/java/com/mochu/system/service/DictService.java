package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.system.dto.DictDataDTO;
import com.mochu.system.dto.DictDataQueryDTO;
import com.mochu.system.dto.DictTypeDTO;
import com.mochu.system.dto.DictTypeQueryDTO;
import com.mochu.system.entity.SysDictData;
import com.mochu.system.entity.SysDictType;
import com.mochu.system.mapper.SysDictDataMapper;
import com.mochu.system.mapper.SysDictTypeMapper;
import com.mochu.system.vo.DictDataVO;
import com.mochu.system.vo.DictTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = "dict:data:";
    private static final long CACHE_TTL_HOURS = 24;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    // ==================== DictType ====================

    public PageResult<DictTypeVO> listDictTypes(DictTypeQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();
        Page<SysDictType> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();

        if (query.getDictType() != null && !query.getDictType().isBlank()) {
            wrapper.like(SysDictType::getDictType, query.getDictType());
        }
        if (query.getDictName() != null && !query.getDictName().isBlank()) {
            wrapper.like(SysDictType::getDictName, query.getDictName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictType::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysDictType::getCreatedAt);

        dictTypeMapper.selectPage(pageParam, wrapper);
        List<DictTypeVO> records = pageParam.getRecords().stream().map(this::toTypeVO).collect(Collectors.toList());
        return new PageResult<>(records, pageParam.getTotal(), page, size);
    }

    public DictTypeVO getDictTypeById(Integer id) {
        SysDictType entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("字典类型不存在");
        }
        return toTypeVO(entity);
    }

    public void createDictType(DictTypeDTO dto) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictType::getDictType, dto.getDictType());
        if (dictTypeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("字典类型已存在");
        }
        SysDictType entity = new SysDictType();
        entity.setDictType(dto.getDictType());
        entity.setDictName(dto.getDictName());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());
        entity.setCreatorId(SecurityUtils.getCurrentUserId());
        dictTypeMapper.insert(entity);
    }

    public void updateDictType(Integer id, DictTypeDTO dto) {
        SysDictType entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("字典类型不存在");
        }
        entity.setDictType(dto.getDictType());
        entity.setDictName(dto.getDictName());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setRemark(dto.getRemark());
        dictTypeMapper.updateById(entity);
    }
    public void deleteDictType(Integer id) {
        SysDictType entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("字典类型不存在");
        }
        dictTypeMapper.deleteById(id);
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, entity.getDictType()));
        stringRedisTemplate.delete(CACHE_PREFIX + entity.getDictType());
    }

    // ==================== DictData ====================

    public PageResult<DictDataVO> listDictData(DictDataQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();

        Page<SysDictData> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();

        if (query.getDictType() != null && !query.getDictType().isBlank()) {
            wrapper.eq(SysDictData::getDictType, query.getDictType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysDictData::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort);

        dictDataMapper.selectPage(pageParam, wrapper);
        List<DictDataVO> records = pageParam.getRecords().stream().map(this::toDataVO).collect(Collectors.toList());
        return new PageResult<>(records, pageParam.getTotal(), page, size);
    }

    public List<DictDataVO> getDictDataByType(String dictType) {
        String cacheKey = CACHE_PREFIX + dictType;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return OBJECT_MAPPER.readValue(cached, new TypeReference<List<DictDataVO>>() {});
            } catch (Exception ignored) {
            }
        }
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
               .eq(SysDictData::getStatus, 1)
               .orderByAsc(SysDictData::getDictSort);
        List<DictDataVO> list = dictDataMapper.selectList(wrapper).stream().map(this::toDataVO).collect(Collectors.toList());
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, OBJECT_MAPPER.writeValueAsString(list), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
        return list;
    }
    public void createDictData(DictDataDTO dto) {
        SysDictData entity = new SysDictData();
        entity.setDictType(dto.getDictType());
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setDictSort(dto.getDictSort() != null ? dto.getDictSort() : 0);
        entity.setCssClass(dto.getCssClass());
        entity.setListClass(dto.getListClass());
        entity.setColorHex(dto.getColorHex());
        entity.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());
        entity.setCreatorId(SecurityUtils.getCurrentUserId());
        dictDataMapper.insert(entity);
        refreshCache(dto.getDictType());
    }

    public void updateDictData(Integer id, DictDataDTO dto) {
        SysDictData entity = dictDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("字典数据不存在");
        }
        String oldDictType = entity.getDictType();
        entity.setDictType(dto.getDictType());
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setDictSort(dto.getDictSort());
        entity.setCssClass(dto.getCssClass());
        entity.setListClass(dto.getListClass());
        entity.setColorHex(dto.getColorHex());
        entity.setIsDefault(dto.getIsDefault());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        entity.setRemark(dto.getRemark());
        dictDataMapper.updateById(entity);
        refreshCache(oldDictType);
        if (!oldDictType.equals(dto.getDictType())) {
            refreshCache(dto.getDictType());
        }
    }

    public void deleteDictData(Integer id) {
        SysDictData entity = dictDataMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("字典数据不存在");
        }
        dictDataMapper.deleteById(id);
        refreshCache(entity.getDictType());
    }

    // ==================== Private ====================

    private void refreshCache(String dictType) {
        String cacheKey = CACHE_PREFIX + dictType;
        stringRedisTemplate.delete(cacheKey);
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDictData::getDictType, dictType)
               .eq(SysDictData::getStatus, 1)
               .orderByAsc(SysDictData::getDictSort);
        List<DictDataVO> list = dictDataMapper.selectList(wrapper).stream().map(this::toDataVO).collect(Collectors.toList());
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, OBJECT_MAPPER.writeValueAsString(list), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    private DictTypeVO toTypeVO(SysDictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictName(entity.getDictName());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private DictDataVO toDataVO(SysDictData entity) {
        DictDataVO vo = new DictDataVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setDictValue(entity.getDictValue());
        vo.setDictSort(entity.getDictSort());
        vo.setCssClass(entity.getCssClass());
        vo.setListClass(entity.getListClass());
        vo.setColorHex(entity.getColorHex());
        vo.setIsDefault(entity.getIsDefault());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatorId(entity.getCreatorId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
