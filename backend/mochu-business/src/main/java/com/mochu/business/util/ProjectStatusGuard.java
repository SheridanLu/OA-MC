package com.mochu.business.util;

import com.mochu.common.exception.BusinessException;

import java.util.Map;
import java.util.Set;

/**
 * V3.2 &sect;4.5.3 项目后期状态操作边界矩阵
 * <p>
 * 定义每个项目状态下允许的业务操作。不在允许列表中的操作将被拒绝。
 * 对于未在矩阵中列出的状态（如 draft、pending、tracking），默认允许。
 */
public class ProjectStatusGuard {

    private static final Map<String, Set<String>> ALLOWED_OPERATIONS = Map.ofEntries(
            Map.entry("active", Set.of(
                    "create_contract", "create_inbound", "create_outbound",
                    "create_return", "create_payment", "create_change", "labor_settlement",
                    "receipt", "invoice", "upload_completion_doc", "doc_archive",
                    "report_view", "progress_report")),
            Map.entry("suspended", Set.of(
                    "receipt", "invoice", "doc_archive", "report_view", "progress_report")),
            Map.entry("completion_accepted", Set.of(
                    "create_payment", "labor_settlement",
                    "receipt", "invoice", "upload_completion_doc", "doc_archive", "report_view")),
            Map.entry("final_accepted", Set.of(
                    "create_payment", "labor_settlement",
                    "receipt", "invoice", "upload_completion_doc", "doc_archive", "report_view")),
            Map.entry("audit_done", Set.of(
                    "receipt", "invoice", "upload_completion_doc", "doc_archive", "report_view")),
            Map.entry("closed", Set.of(
                    "doc_archive", "report_view")),
            Map.entry("terminated", Set.of(
                    "doc_archive", "report_view"))
    );

    /**
     * Check if an operation is allowed for the given project status.
     *
     * @param projectStatus current project status
     * @param operation     the operation being attempted
     * @throws BusinessException if operation is not allowed (error code 10001)
     */
    public static void checkAllowed(String projectStatus, String operation) {
        if (projectStatus == null) return; // no project context
        Set<String> allowed = ALLOWED_OPERATIONS.get(projectStatus);
        if (allowed == null) return; // unknown status, allow (e.g., draft, pending, tracking)
        if (!allowed.contains(operation)) {
            throw new BusinessException(10001,
                    String.format("项目状态[%s]不允许执行[%s]操作", projectStatus, operation));
        }
    }

    /**
     * Check if an operation is allowed (non-throwing version).
     *
     * @param projectStatus current project status
     * @param operation     the operation being attempted
     * @return true if allowed, false otherwise
     */
    public static boolean isAllowed(String projectStatus, String operation) {
        if (projectStatus == null) return true;
        Set<String> allowed = ALLOWED_OPERATIONS.get(projectStatus);
        if (allowed == null) return true;
        return allowed.contains(operation);
    }
}
