package com.hsj.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file, String directory);

    void delete(String filePath);

    String getFileUrl(String filePath);
}
