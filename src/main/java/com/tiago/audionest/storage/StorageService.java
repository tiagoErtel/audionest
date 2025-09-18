package com.tiago.audionest.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class StorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public Resource loadAsResource(String filename) {
        Path path = Paths.get(uploadDir).resolve(filename);
        return new FileSystemResource(path.toFile());
    }

    public String store(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir))
            Files.createDirectories(dir);

        String filename = System.currentTimeMillis() + "_" + Path.of(file.getOriginalFilename()).getFileName();
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}
