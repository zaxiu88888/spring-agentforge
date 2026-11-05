package com.agentforge.service.impl;

import com.agentforge.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class DocumentStorageServiceImpl implements DocumentStorageService {

    private final Path basePath;

    public DocumentStorageServiceImpl(@Value("${document.storage.base-path:./data/documents}") String basePath) {
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public String saveFile(String kbId, String documentId, MultipartFile file) throws IOException {
        Path dir = basePath.resolve(kbId).resolve(documentId);
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + ".md");
        file.transferTo(target);
        return basePath.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public void deleteDirectory(String kbId, String documentId) {
        Path dir = basePath.resolve(kbId).resolve(documentId);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("删除文件失败: {}", p, e);
                }
            });
        } catch (IOException e) {
            log.warn("删除文档目录失败: {}", dir, e);
        }
        // 清理空的 KB 父目录
        Path kbDir = basePath.resolve(kbId);
        try (Stream<Path> list = Files.list(kbDir)) {
            if (list.findAny().isEmpty()) {
                Files.delete(kbDir);
            }
        } catch (IOException ignored) {
            // KB 目录不存在或非空，忽略
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        return Files.exists(basePath.resolve(relativePath));
    }
}
