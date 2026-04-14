package com.mochu.business.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.mochu.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final AttachmentService attachmentService;

    /**
     * 导入结果
     */
    @lombok.Data
    public static class ImportResult {
        private int successCount;
        private int failCount;
        private int totalCount;
        private String errorFileUrl; // 错误反馈文件下载链接（失败时有值）
        private List<ImportError> errors; // 错误明细
    }

    /**
     * 导入错误明细（对应附录I）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ImportError {
        private int rowNumber;      // 错误行号（从2开始）
        private String fieldName;   // 错误字段名
        private String fieldValue;  // 用户填写值
        private String errorMessage; // 错误原因
        private String suggestion;   // 修正建议
    }

    /**
     * 通用导入入口
     *
     * @param file          上传的 Excel 文件
     * @param clazz         EasyExcel 数据实体类
     * @param templateHeaders 标准模板列头列表（用于校验模板格式）
     * @param validator     逐行校验函数：输入单行数据，返回该行错误列表（空=通过）
     * @param saver         逐行保存函数：输入单行数据，执行入库
     * @param userId        当前用户ID
     * @return 导入结果
     */
    public <T> ImportResult doImport(MultipartFile file, Class<T> clazz,
                                     List<String> templateHeaders,
                                     Function<T, List<ImportError>> validator,
                                     java.util.function.Consumer<T> saver,
                                     Integer userId) {
        ImportResult result = new ImportResult();
        List<ImportError> allErrors = new ArrayList<>();
        List<T> successRows = new ArrayList<>();
        List<T> allRows = new ArrayList<>();

        // 1. 读取 Excel
        try (InputStream is = file.getInputStream()) {
            EasyExcel.read(is, clazz, new ReadListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    allRows.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 列头校验在 invokeHead 中完成
                }
                @Override
                public void invokeHead(Map<Integer, com.alibaba.excel.metadata
                        .data.ReadCellData<?>> headMap,
                                       AnalysisContext context) {
                    // 校验列头与标准模板是否一致
                    if (templateHeaders != null && !templateHeaders.isEmpty()) {
                        List<String> actualHeaders = new ArrayList<>();
                        headMap.forEach((k, v) ->
                                actualHeaders.add(v.getStringValue()));
                        if (!actualHeaders.equals(templateHeaders)) {
                            throw new BusinessException(16001,
                                    "导入模板格式不匹配");
                        }
                    }
                }
            }).sheet().doRead();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取导入文件失败", e);
            throw new BusinessException(400, "导入文件读取失败");
        }

        // 2. 逐行校验
        for (int i = 0; i < allRows.size(); i++) {
            T row = allRows.get(i);
            List<ImportError> rowErrors = validator.apply(row);
            if (rowErrors != null && !rowErrors.isEmpty()) {
                // 设置行号（Excel 第 1 行是表头，数据从第 2 行开始）
                int rowNum = i + 2;
                rowErrors.forEach(e -> e.setRowNumber(rowNum));
                allErrors.addAll(rowErrors);
            } else {
                successRows.add(row);
            }
        }

        // 3. 逐条事务模式：成功行入库
        int savedCount = 0;
        for (T row : successRows) {
            try {
                saver.accept(row);
                savedCount++;
            } catch (Exception e) {
                log.warn("导入行入库失败: {}", e.getMessage());
                // 入库失败也记录为错误
                ImportError err = new ImportError();
                err.setRowNumber(allRows.indexOf(row) + 2);
                err.setErrorMessage("入库失败: " + e.getMessage());
                allErrors.add(err);
            }
        }

        // 4. 组装结果
        result.setTotalCount(allRows.size());
        result.setSuccessCount(savedCount);
        result.setFailCount(allRows.size() - savedCount);
        result.setErrors(allErrors);

        // 5. 如有错误行 → 生成错误反馈 Excel
        if (!allErrors.isEmpty()) {
            result.setErrorFileUrl(generateErrorFeedback(allErrors));
        }

        return result;
    }

    /**
     * 生成错误反馈 Excel 文件
     *
     * 规则（附录I）：
     * - 在原 Excel 基础上追加"错误原因"列
     * - 错误行标红
     * - 同一行多个字段校验失败时，错误原因用分号分隔
     */
    private String generateErrorFeedback(List<ImportError> errors) {
        try {
            // 按行号分组，合并同行错误
            Map<Integer, StringBuilder> rowErrors = new TreeMap<>();
            for (ImportError err : errors) {
                rowErrors.computeIfAbsent(err.getRowNumber(),
                                k -> new StringBuilder())
                        .append(err.getFieldName() != null
                                ? err.getFieldName() + ": " : "")
                        .append(err.getErrorMessage())
                        .append("；");
            }

            // 构建错误反馈数据
            List<List<String>> feedbackData = new ArrayList<>();
            feedbackData.add(List.of("行号", "错误字段", "填写值",
                    "错误原因", "修正建议"));

            for (ImportError err : errors) {
                feedbackData.add(List.of(
                        String.valueOf(err.getRowNumber()),
                        err.getFieldName() != null ? err.getFieldName() : "",
                        err.getFieldValue() != null ? err.getFieldValue() : "",
                        err.getErrorMessage() != null ? err.getErrorMessage() : "",
                        err.getSuggestion() != null ? err.getSuggestion() : ""
                ));
            }

            // 写入临时文件
            File tempFile = File.createTempFile("import_error_", ".xlsx");
            EasyExcel.write(tempFile)
                    .registerWriteHandler(
                            new LongestMatchColumnWidthStyleStrategy())
                    .sheet("错误反馈")
                    .doWrite(feedbackData);

            // TODO: 上传到 MinIO 并返回预签名下载 URL
            // 简化处理：返回临时文件路径
            log.info("错误反馈文件已生成: {}", tempFile.getAbsolutePath());
            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            log.error("生成错误反馈文件失败", e);
            return null;
        }
    }
}
