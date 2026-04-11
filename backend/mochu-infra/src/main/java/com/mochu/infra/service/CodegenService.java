package com.mochu.infra.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.common.constant.Constants;
import com.mochu.common.exception.BusinessException;
import com.mochu.common.result.PageResult;
import com.mochu.common.security.SecurityUtils;
import com.mochu.infra.codegen.CodegenBuilder;
import com.mochu.infra.codegen.CodegenEngine;
import com.mochu.infra.dto.CodegenColumnDTO;
import com.mochu.infra.dto.CodegenTableDTO;
import com.mochu.infra.dto.CodegenTableQueryDTO;
import com.mochu.infra.entity.InfraCodegenColumn;
import com.mochu.infra.entity.InfraCodegenTable;
import com.mochu.infra.mapper.InfraCodegenColumnMapper;
import com.mochu.infra.mapper.InfraCodegenTableMapper;
import com.mochu.infra.vo.CodegenColumnVO;
import com.mochu.infra.vo.CodegenTableVO;
import com.mochu.infra.vo.DbTableVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodegenService {

    private final InfraCodegenTableMapper tableMapper;
    private final InfraCodegenColumnMapper columnMapper;
    private final CodegenBuilder codegenBuilder;
    private final CodegenEngine codegenEngine;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    // ==================== 表管理 ====================

    public PageResult<CodegenTableVO> listTables(CodegenTableQueryDTO query) {
        int page = (query.getPage() == null || query.getPage() < 1) ? Constants.DEFAULT_PAGE : query.getPage();
        int size = (query.getSize() == null || query.getSize() < 1) ? Constants.DEFAULT_SIZE : query.getSize();
        Page<InfraCodegenTable> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InfraCodegenTable> wrapper = new LambdaQueryWrapper<>();
        if (query.getTableName() != null && !query.getTableName().isBlank()) {
            wrapper.like(InfraCodegenTable::getTableName, query.getTableName());
        }
        wrapper.orderByDesc(InfraCodegenTable::getCreatedAt);
        tableMapper.selectPage(pageParam, wrapper);
        List<CodegenTableVO> records = pageParam.getRecords().stream().map(this::toTableVO).collect(Collectors.toList());
        return new PageResult<>(records, pageParam.getTotal(), page, size);
    }

    /** 查询数据库中尚未导入的表 */
    public List<DbTableVO> listDbTables() {
        String dbName = extractDbName(datasourceUrl);
        List<String> imported = tableMapper.selectList(null).stream()
                .map(InfraCodegenTable::getTableName).collect(Collectors.toList());
        return codegenBuilder.listUnimportedTables(dbName, imported);
    }

    @Transactional
    public void importTable(String tableName, String author) {
        LambdaQueryWrapper<InfraCodegenTable> check = new LambdaQueryWrapper<>();
        check.eq(InfraCodegenTable::getTableName, tableName);
        if (tableMapper.selectCount(check) > 0) {
            throw new BusinessException("该表已导入");
        }
        String dbName = extractDbName(datasourceUrl);
        String tableComment = codegenBuilder.getTableComment(dbName, tableName);

        InfraCodegenTable table = new InfraCodegenTable();
        table.setTableName(tableName);
        table.setTableComment(tableComment);
        table.setModuleName("business");
        // 去掉前缀推断业务名（biz_contract -> contract）
        String bizName = tableName.replaceFirst("^(biz_|sys_|infra_)", "");
        table.setBizName(bizName);
        table.setClassName(CodegenBuilder.capitalize(CodegenBuilder.toCamelCase(bizName)));
        table.setTemplateType(1);
        table.setAuthor(author != null ? author : "mochu");
        table.setCreatorId(SecurityUtils.getCurrentUserId());
        tableMapper.insert(table);

        List<InfraCodegenColumn> columns = codegenBuilder.buildColumns(dbName, tableName, table);
        columns.forEach(col -> {
            col.setCreatorId(SecurityUtils.getCurrentUserId());
            columnMapper.insert(col);
        });
    }

    public CodegenTableVO getTableDetail(Integer id) {
        InfraCodegenTable table = tableMapper.selectById(id);
        if (table == null) throw new BusinessException("表配置不存在");
        CodegenTableVO vo = toTableVO(table);
        vo.setColumns(listColumnsVO(id));
        return vo;
    }

    public void updateTable(Integer id, CodegenTableDTO dto) {
        InfraCodegenTable table = tableMapper.selectById(id);
        if (table == null) throw new BusinessException("表配置不存在");
        table.setModuleName(dto.getModuleName());
        table.setBizName(dto.getBizName());
        table.setClassName(dto.getClassName());
        if (dto.getTemplateType() != null) table.setTemplateType(dto.getTemplateType());
        if (dto.getAuthor() != null) table.setAuthor(dto.getAuthor());
        if (dto.getRemark() != null) table.setRemark(dto.getRemark());
        tableMapper.updateById(table);
    }

    @Transactional
    public void deleteTable(Integer id) {
        tableMapper.deleteById(id);
        columnMapper.delete(new LambdaQueryWrapper<InfraCodegenColumn>()
                .eq(InfraCodegenColumn::getTableId, id));
    }

    // ==================== 列管理 ====================

    public List<CodegenColumnVO> listColumnsVO(Integer tableId) {
        return columnMapper.selectList(new LambdaQueryWrapper<InfraCodegenColumn>()
                .eq(InfraCodegenColumn::getTableId, tableId)
                .orderByAsc(InfraCodegenColumn::getColumnSort))
                .stream().map(this::toColumnVO).collect(Collectors.toList());
    }

    @Transactional
    public void updateColumns(Integer tableId, List<CodegenColumnDTO> dtos) {
        // 全量替换列配置
        columnMapper.delete(new LambdaQueryWrapper<InfraCodegenColumn>()
                .eq(InfraCodegenColumn::getTableId, tableId));
        int sort = 0;
        for (CodegenColumnDTO dto : dtos) {
            InfraCodegenColumn col = new InfraCodegenColumn();
            col.setTableId(tableId);
            col.setColumnName(dto.getColumnName());
            col.setColumnComment(dto.getColumnComment());
            col.setDataType(dto.getDataType());
            col.setJavaType(dto.getJavaType());
            col.setJavaField(dto.getJavaField());
            col.setDictType(dto.getDictType());
            col.setHtmlType(dto.getHtmlType());
            col.setPkFlag(dto.getPkFlag() != null ? dto.getPkFlag() : 0);
            col.setNullableFlag(dto.getNullableFlag() != null ? dto.getNullableFlag() : 1);
            col.setCreateOperation(dto.getCreateOperation() != null ? dto.getCreateOperation() : 1);
            col.setUpdateOperation(dto.getUpdateOperation() != null ? dto.getUpdateOperation() : 1);
            col.setListOperation(dto.getListOperation() != null ? dto.getListOperation() : 1);
            col.setQueryOperation(dto.getQueryOperation() != null ? dto.getQueryOperation() : 0);
            col.setQueryCondition(dto.getQueryCondition() != null ? dto.getQueryCondition() : "EQ");
            col.setColumnSort(sort++);
            col.setCreatorId(SecurityUtils.getCurrentUserId());
            columnMapper.insert(col);
        }
    }

    // ==================== 代码生成 ====================

    public Map<String, String> previewCode(Integer tableId) {
        InfraCodegenTable table = tableMapper.selectById(tableId);
        if (table == null) throw new BusinessException("表配置不存在");
        List<InfraCodegenColumn> columns = columnMapper.selectList(
                new LambdaQueryWrapper<InfraCodegenColumn>()
                        .eq(InfraCodegenColumn::getTableId, tableId)
                        .orderByAsc(InfraCodegenColumn::getColumnSort));
        return codegenEngine.preview(table, columns);
    }

    public byte[] downloadCode(Integer tableId) {
        InfraCodegenTable table = tableMapper.selectById(tableId);
        if (table == null) throw new BusinessException("表配置不存在");
        List<InfraCodegenColumn> columns = columnMapper.selectList(
                new LambdaQueryWrapper<InfraCodegenColumn>()
                        .eq(InfraCodegenColumn::getTableId, tableId)
                        .orderByAsc(InfraCodegenColumn::getColumnSort));
        return codegenEngine.generateZip(table, columns);
    }

    // ==================== 私有方法 ====================

    private String extractDbName(String url) {
        // jdbc:mysql://host:port/dbName?...
        try {
            String path = url.substring(url.lastIndexOf('/') + 1);
            int q = path.indexOf('?');
            return q > 0 ? path.substring(0, q) : path;
        } catch (Exception e) {
            return "mochu_oa";
        }
    }

    private CodegenTableVO toTableVO(InfraCodegenTable e) {
        CodegenTableVO vo = new CodegenTableVO();
        vo.setId(e.getId());
        vo.setTableName(e.getTableName());
        vo.setTableComment(e.getTableComment());
        vo.setModuleName(e.getModuleName());
        vo.setBizName(e.getBizName());
        vo.setClassName(e.getClassName());
        vo.setTemplateType(e.getTemplateType());
        vo.setAuthor(e.getAuthor());
        vo.setRemark(e.getRemark());
        vo.setCreatorId(e.getCreatorId());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    private CodegenColumnVO toColumnVO(InfraCodegenColumn e) {
        CodegenColumnVO vo = new CodegenColumnVO();
        vo.setId(e.getId());
        vo.setTableId(e.getTableId());
        vo.setColumnName(e.getColumnName());
        vo.setColumnComment(e.getColumnComment());
        vo.setDataType(e.getDataType());
        vo.setJavaType(e.getJavaType());
        vo.setJavaField(e.getJavaField());
        vo.setDictType(e.getDictType());
        vo.setHtmlType(e.getHtmlType());
        vo.setPkFlag(e.getPkFlag());
        vo.setNullableFlag(e.getNullableFlag());
        vo.setCreateOperation(e.getCreateOperation());
        vo.setUpdateOperation(e.getUpdateOperation());
        vo.setListOperation(e.getListOperation());
        vo.setQueryOperation(e.getQueryOperation());
        vo.setQueryCondition(e.getQueryCondition());
        vo.setColumnSort(e.getColumnSort());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }
}
