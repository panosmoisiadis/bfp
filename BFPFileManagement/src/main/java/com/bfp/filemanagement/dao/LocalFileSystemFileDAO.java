package com.bfp.filemanagement.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class LocalFileSystemFileDAO implements FileDAO {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileSystemFileDAO.class);

    private final String fileStorageLocation;

    public LocalFileSystemFileDAO(String fileStorageLocation) {
        this.fileStorageLocation = fileStorageLocation;
    }

    @Override
    public InputStream getFileSteam(String fileId) {
        File file = new File(generatePath(fileId).toString());
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            return fileInputStream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getFileBytes(String fileId) {
        File file = new File(generatePath(fileId).toString());
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileContent = fileInputStream.readAllBytes();
            fileInputStream.close();
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createFile(String fileId, MultipartFile file) throws IOException {
        Path path = generatePath(fileId);
        file.transferTo(path);
        return path.toString();
    }

    @Override
    public void deleteFile(String fileId) {
        File file = new File(generatePath(fileId).toString());
        if (!file.exists()) {
            return;
        }
        file.delete();
    }

    @Override
    public void updateFile(String fileId, MultipartFile file) throws IOException {
        Path path = generatePath(fileId);
        file.transferTo(path);
    }

    private Path generatePath(String fileId) {
        return Path.of(fileStorageLocation, fileId).toAbsolutePath();
    }
}
