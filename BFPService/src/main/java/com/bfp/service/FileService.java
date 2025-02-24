package com.bfp.service;

import com.bfp.filemanagement.FileHandler;
import com.bfp.model.BFPFile;
import com.bfp.model.CreateFileResponse;
import com.bfp.model.ListedBFPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class FileService {

    @Autowired
    private CommonRequestHelper commonRequestHelper;

    private final FileHandler fileHandler;

    @Autowired
    public FileService(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @PostMapping("/file")
    public CreateFileResponse createFile(final MultipartFile file) throws IOException {
        return fileHandler.createFile(file);
    }

    @GetMapping("/file/{id}/info")
    public BFPFile getFileInfo(@PathVariable("id") String fileId) {
        return fileHandler.getFileInfo(fileId);
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<InputStreamResource> getFile(@PathVariable("id") String fileId) {
        InputStreamResource resource = new InputStreamResource(fileHandler.downloadFileStream(fileId));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PutMapping("/file/{id}")
    public BFPFile updateFile(@PathVariable("id") final String fileId, final MultipartFile file) throws IOException {
        return fileHandler.updateFile(fileId, file);
    }

    @DeleteMapping("/file/{id}")
    public void deleteFile(@PathVariable("id") final String fileId) {
        fileHandler.deleteFile(fileId);
    }

    @GetMapping("/files")
    public List<ListedBFPFile> listFiles() {
        return fileHandler.listFiles();
    }
}
