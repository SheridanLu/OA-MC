package com.mochu.common.util;

import com.mochu.common.constant.FileConstants;
import com.mochu.common.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件上传安全校验
 * 双重校验：扩展名 + 文件头魔数
 */
public final class FileSecurityUtil {

    private FileSecurityUtil() {}

    /**
     * 校验单个文件：大小 + 扩展名 + 魔数
     *
     * @throws BusinessException 15001 文件类型不允许 / 15002 文件大小超过限制
     */
    public static void validate(MultipartFile file) {
        // 1. 文件大小校验
        if (file.getSize() > FileConstants.MAX_FILE_SIZE) {
            throw new BusinessException(15002, "文件大小超过限制");
        }

        // 2. 扩展名白名单
        String ext = getExtension(file.getOriginalFilename());
        if (!FileConstants.ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(15001, "文件类型不允许");
        }

        // 3. MIME 类型基本校验（防御 Content-Type 伪造不可靠，以魔数为准）
        // 4. 文件头魔数校验
        validateMagicNumber(file, ext);
    }

    /**
     * 批量校验
     */
    public static void validateBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        if (files.length > FileConstants.MAX_BATCH_COUNT) {
            throw new BusinessException(400,
                    "批量上传最多" + FileConstants.MAX_BATCH_COUNT + "个文件");
        }
        for (MultipartFile file : files) {
            validate(file);
        }
    }

    /**
     * 提取文件扩展名（小写）
     */
    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 文件头魔数校验 — 防止伪装文件
     */
    private static void validateMagicNumber(MultipartFile file, String ext) {
        String[] magics = FileConstants.MAGIC_NUMBERS.get(ext);
        if (magics == null || magics.length == 0) {
            return; // txt 等无固定魔数，跳过
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8]; // 读取前 8 字节
            int read = is.read(header);
            if (read < 4) {
                throw new BusinessException(15001, "文件类型不允许");
            }
            String hex = bytesToHex(header, read);

            boolean matched = false;
            for (String magic : magics) {
                if (hex.startsWith(magic)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new BusinessException(15001, "文件类型不允许");
            }
        } catch (IOException e) {
            throw new BusinessException(15001, "文件类型不允许");
        }
    }

    private static String bytesToHex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
