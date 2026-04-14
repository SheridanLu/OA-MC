package com.mochu.business.scheduler;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * 异步导出文件清理 — 保留 7 天
 * Cron: 0 30 2 * * ?（每日 02:30）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExportFileCleanupJob {

    private final MinioClient minioClient;

    @Value("${minio.bucket:mochu-oa}")
    private String bucket;

    @XxlJob("exportFileCleanupJob")
    public void execute() {
        log.info("开始清理过期导出文件...");
        int count = 0;
        ZonedDateTime threshold = ZonedDateTime.now().minusDays(7);

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix("export/")
                            .recursive(true)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.lastModified().isBefore(threshold)) {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build());
                    count++;
                }
            }
            log.info("清理过期导出文件完成，共删除 {} 个文件", count);
        } catch (Exception e) {
            log.error("清理过期导出文件失败", e);
        }
    }
}
