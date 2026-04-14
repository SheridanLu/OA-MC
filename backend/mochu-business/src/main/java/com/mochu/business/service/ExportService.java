package com.mochu.business.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.mochu.common.exception.BusinessException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final MinioClient minioClient;
    private final TodoService todoService;

    /** 同步导出阈值 */
    private static final int SYNC_THRESHOLD = 5000;

    /** 异步导出文件保留天数 */
    private static final int ASYNC_FILE_KEEP_DAYS = 7;

    @Value("${minio.bucket:mochu-oa}")
    private String bucket;

    /**
     * 导出入口 — 根据总数自动分流同步/异步
     *
     * @param totalCount  数据总量
     * @param fileName    导出文件名（不含扩展名）
     * @param clazz       EasyExcel 实体类
     * @param dataSupplier 数据获取函数（延迟执行）
     * @param response    HTTP 响应（同步模式使用）
     * @param userId      当前用户ID
     * @return null=同步已写入 response，非null=异步任务已提交
     */
    public <T> String export(long totalCount, String fileName,
                             Class<T> clazz, Supplier<List<T>> dataSupplier,
                             HttpServletResponse response, Integer userId) {
        if (totalCount <= SYNC_THRESHOLD) {
            // ≤5000 条 → 同步导出
            syncExport(fileName, clazz, dataSupplier.get(), response);
            return null;
        } else {
            // >5000 条 → 异步导出
            asyncExport(fileName, clazz, dataSupplier, userId);
            return "导出任务已提交，完成后将推送通知";
        }
    }

    /**
     * 同步导出 — 直接写入 HTTP 响应流
     */
    public <T> void syncExport(String fileName, Class<T> clazz,
                               List<T> data, HttpServletResponse response) {
        try {
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename*=utf-8''" + encodedName + ".xlsx");

            EasyExcel.write(response.getOutputStream(), clazz)
                    .registerWriteHandler(
                            new LongestMatchColumnWidthStyleStrategy())
                    .sheet("数据")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("同步导出失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "导出失败");
        }
    }

    /**
     * 异步导出 — 生成文件到 MinIO，完成后推送通知
     * 文件保留 7 天后由定时任务清理
     */
    @Async("exportTaskExecutor")
    public <T> void asyncExport(String fileName, Class<T> clazz,
                                Supplier<List<T>> dataSupplier,
                                Integer userId) {
        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMM"));
        String objectPath = "export/" + datePath + "/" + fileName
                + "_" + System.currentTimeMillis() + ".xlsx";

        File tempFile = null;
        try {
            // 写入临时文件
            tempFile = File.createTempFile("export_", ".xlsx");
            List<T> data = dataSupplier.get();

            EasyExcel.write(tempFile, clazz)
                    .registerWriteHandler(
                            new LongestMatchColumnWidthStyleStrategy())
                    .sheet("数据")
                    .doWrite(data);

            // 上传到 MinIO
            try (InputStream is = new FileInputStream(tempFile)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .stream(is, tempFile.length(), -1)
                        .contentType("application/vnd.openxmlformats-officedocument"
                                + ".spreadsheetml.sheet")
                        .build());
            }

            // 推送通知 — 通过待办系统
            todoService.createTodo(userId, "export_complete",
                    null, "导出完成",
                    "文件【" + fileName + ".xlsx】已生成，请在7天内下载。",
                    "/" + bucket + "/" + objectPath);

            log.info("异步导出完成: userId={}, path={}", userId, objectPath);

        } catch (Exception e) {
            log.error("异步导出失败: userId={}, fileName={}",
                    userId, fileName, e);
            // 推送失败通知
            todoService.createTodo(userId, "export_failed",
                    null, "导出失败",
                    "文件【" + fileName + ".xlsx】导出失败，请重试。",
                    null);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
