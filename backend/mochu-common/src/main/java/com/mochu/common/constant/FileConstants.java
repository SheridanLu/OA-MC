package com.mochu.common.constant;

import java.util.Map;
import java.util.Set;

/**
 * 文件上传安全常量
 */
public final class FileConstants {

    private FileConstants() {}

    /** 单文件最大字节 50MB */
    public static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    /** 批量上传每批最多文件数 */
    public static final int MAX_BATCH_COUNT = 10;

    /** 允许的文件扩展名（小写） */
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "png", "gif", "webp",
            "pdf", "doc", "docx", "xls", "xlsx",
            "txt", "zip", "rar"
    );

    /**
     * 文件头魔数映射（扩展名 → 首字节 hex 前缀）
     * 用于双重校验：扩展名 + 文件头
     */
    public static final Map<String, String[]> MAGIC_NUMBERS = Map.ofEntries(
            Map.entry("jpg",  new String[]{"FFD8FF"}),
            Map.entry("png",  new String[]{"89504E47"}),
            Map.entry("gif",  new String[]{"47494638"}),
            Map.entry("webp", new String[]{"52494646"}),       // RIFF header
            Map.entry("pdf",  new String[]{"25504446"}),       // %PDF
            Map.entry("doc",  new String[]{"D0CF11E0"}),       // OLE
            Map.entry("docx", new String[]{"504B0304"}),       // PK (ZIP)
            Map.entry("xls",  new String[]{"D0CF11E0"}),       // OLE
            Map.entry("xlsx", new String[]{"504B0304"}),       // PK (ZIP)
            Map.entry("txt",  new String[]{}),                 // 纯文本无固定魔数
            Map.entry("zip",  new String[]{"504B0304", "504B0506", "504B0708"}),
            Map.entry("rar",  new String[]{"526172211A07"})
    );

    /** biz_type 合法枚举值 */
    public static final Set<String> ALLOWED_BIZ_TYPES = Set.of(
            "change_visa", "change_owner", "change_overage", "change_labor_visa",
            "labor_settlement", "contract_income", "contract_expense",
            "contract_supplement", "project", "bid_notice",
            "inbound", "outbound", "return", "inventory_check",
            "spot_purchase", "completion_finish", "completion_drawing",
            "reimburse", "template_income", "template_expense",
            "purchase_list", "payment_apply", "payment_receipt"
    );

    /** MinIO 预签名 URL 有效期（秒） */
    public static final int PRESIGN_EXPIRE_SECONDS = 3600;
}
