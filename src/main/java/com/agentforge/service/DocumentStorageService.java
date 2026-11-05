package com.agentforge.service;

import org.springframework.web.multipart.MultipartFile;

/** 文档文件落盘/清理（路径协议：{base}/{kbId}/{docId}/{uuid}.md，tech-design.md §3 与原型一致） */
public interface DocumentStorageService {

    /** @return 相对存储路径（正斜杠），写入 document.metadata.filePath */
    String saveFile(String kbId, String documentId, MultipartFile file) throws Exception;

    /** 删除文档目录并尝试清理空的 KB 父目录 */
    void deleteDirectory(String kbId, String documentId);

    boolean exists(String relativePath);
}
