package com.bfp.filemanagement.dao;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileDAO {
    byte[] getFileBytes(String fileId);
    InputStream getFileSteam(String fileId);
    String createFile(String fileId, MultipartFile file) throws IOException;
    void deleteFile(String fileId);
    void updateFile(String fileId, MultipartFile file) throws IOException;
}
