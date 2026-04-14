package com.mochu.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochu.common.constant.Constants;
import com.mochu.common.result.PageResult;
import com.mochu.system.entity.SysAuditLog;
import com.mochu.system.mapper.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志
     */
    public PageResult<SysAuditLog> list(String operateModule, String operateType,
                                         Integer userId, LocalDate startDate, LocalDate endDate,
                                         Integer page, Integer size) {
        if (page == null || page < 1) page = Constants.DEFAULT_PAGE;
        if (size == null || size < 1) size = Constants.DEFAULT_SIZE;

        Page<SysAuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();

        if (operateModule != null && !operateModule.isBlank()) {
            wrapper.eq(SysAuditLog::getOperateModule, operateModule);
        }
        if (operateType != null && !operateType.isBlank()) {
            wrapper.eq(SysAuditLog::getOperateType, operateType);
        }
        if (userId != null) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (startDate != null) {
            wrapper.ge(SysAuditLog::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(SysAuditLog::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysAuditLog::getCreatedAt);

        auditLogMapper.selectPage(pageParam, wrapper);
        return new PageResult<>(pageParam.getRecords(), pageParam.getTotal(), page, size);
    }

    /**
     * 记录审计日志（内部方法）
     */
    public void log(Integer userId, String userName, String operateType,
                    String operateModule, String bizType, Integer bizId,
                    String beforeData, String afterData, String ipAddress, String requestId) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setOperateType(operateType);
        log.setOperateModule(operateModule);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setIpAddress(ipAddress);
        log.setRequestId(requestId);
        auditLogMapper.insert(log);
    }

    /**
     * 导出审计日志为CSV
     */
    public void exportLogs(String module, String startDate, String endDate,
                           HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) {
            wrapper.eq(SysAuditLog::getOperateModule, module);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(SysAuditLog::getCreatedAt, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(SysAuditLog::getCreatedAt, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(SysAuditLog::getCreatedAt);

        List<SysAuditLog> logs = auditLogMapper.selectList(wrapper);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=audit_logs.csv");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        // UTF-8 BOM for Excel compatibility
        writer.write('\ufeff');
        writer.println("ID,用户ID,用户名,操作类型,操作模块,业务类型,业务ID,IP地址,请求ID,操作时间");

        for (SysAuditLog logEntry : logs) {
            writer.println(
                    escapeCsv(logEntry.getId()) + ","
                    + escapeCsv(logEntry.getUserId()) + ","
                    + escapeCsv(logEntry.getUserName()) + ","
                    + escapeCsv(logEntry.getOperateType()) + ","
                    + escapeCsv(logEntry.getOperateModule()) + ","
                    + escapeCsv(logEntry.getBizType()) + ","
                    + escapeCsv(logEntry.getBizId()) + ","
                    + escapeCsv(logEntry.getIpAddress()) + ","
                    + escapeCsv(logEntry.getRequestId()) + ","
                    + escapeCsv(logEntry.getCreatedAt()));
        }

        writer.flush();
    }

    private String escapeCsv(Object value) {
        if (value == null) return "";
        String str = value.toString();
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
