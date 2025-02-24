package com.bfp.filemanagement;

import com.bfp.common.CommonRequestHelper;
import com.bfp.filemanagement.dao.FileDAO;
import com.bfp.filemanagement.dao.FileMetadataDAO;
import com.bfp.filemanagement.dao.FileDO;
import com.bfp.model.BFPFile;
import com.bfp.model.CreateFileResponse;
import com.bfp.model.ListedBFPFile;
import com.bfp.model.exceptions.InvalidParameterException;
import com.bfp.model.exceptions.ResourceNotFoundException;
import com.bfp.model.exceptions.UnauthorizedException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FileHandler {
    private final FileMetadataDAO fileMetadataDAO;
    private final FileDAO fileDAO;

    public FileHandler(FileMetadataDAO fileMetadataDAO, FileDAO fileDAO) {
        this.fileMetadataDAO = fileMetadataDAO;
        this.fileDAO = fileDAO;
    }

    public CreateFileResponse createFile(MultipartFile uploadFile) throws IOException {
        // Implementation for handling file upload

        if (uploadFile == null) {
            throw new InvalidParameterException("File cannot be null");
        }

        String ownerId = CommonRequestHelper.getUserId();
        UUID fileId = UUID.randomUUID();
        String filename = uploadFile.getOriginalFilename();
        Instant createdAt = Instant.now();
        String contentType = uploadFile.getContentType();
        String fileLocation = fileDAO.createFile(fileId.toString(), uploadFile);


        FileDO fileDO = FileDO.builder()
                .id(fileId)
                .ownerId(ownerId)
                .fileLocation(fileLocation)
                .fileName(filename)
                .fileSize(uploadFile.getSize())
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .contentType(contentType)
                .build();

        fileMetadataDAO.saveFile(fileDO);

        return CreateFileResponse.builder()
                .id(fileId.toString())
                .build();
    }

    public BFPFile getFileInfo(String fileId) {
        Optional<FileDO> filePromise =  fileMetadataDAO.getFile(UUID.fromString(fileId));

        if (filePromise.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        FileDO file = filePromise.get();

        String ownerId = CommonRequestHelper.getUserId();

        if (!file.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You are not authorized to access this file.");
        }

        BFPFile bfpFile = convertFileDOToModelFile(file);

        return bfpFile;
    }

    public byte[] downloadFile(String fileId) {
        Optional<FileDO> file =  fileMetadataDAO.getFile(UUID.fromString(fileId));

        if (file.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        FileDO fileDO = file.get();

        verifyFileOwner(fileDO);

        byte[] fileContent = fileDAO.getFileBytes(fileDO.getId().toString());

        return fileContent;
    }

    public InputStream downloadFileStream(String fileId) {
        Optional<FileDO> file =  fileMetadataDAO.getFile(UUID.fromString(fileId));

        if (file.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        FileDO fileDO = file.get();

        verifyFileOwner(fileDO);

        InputStream fileContent = fileDAO.getFileSteam(fileDO.getId().toString());

        return fileContent;
    }

    public BFPFile updateFile(String fileId, MultipartFile newFile) throws IOException {
        Optional<FileDO> file =  fileMetadataDAO.getFile(UUID.fromString(fileId));

        if (file.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        FileDO fileDO = file.get();

        verifyFileOwner(fileDO);

        fileDAO.updateFile(fileId, newFile);

        updateFileDO(fileDO, newFile);

        fileMetadataDAO.updateFile(fileDO);

        BFPFile bfpFile = convertFileDOToModelFile(fileDO);

        return bfpFile;
    }

    public void deleteFile(String fileId) {
        Optional<FileDO> file =  fileMetadataDAO.getFile(UUID.fromString(fileId));

        if (file.isEmpty()) {
            throw new ResourceNotFoundException();
        }

        FileDO fileDO = file.get();

        verifyFileOwner(fileDO);

        fileDAO.deleteFile(fileDO.getId().toString());

        fileMetadataDAO.deleteFile(fileDO.getId());
    }

    public List<ListedBFPFile> listFiles() {
        String ownerId = CommonRequestHelper.getUserId();
        List<FileDO> fileList = fileMetadataDAO.getAllFilesByOwnerId(ownerId);
        List<ListedBFPFile> modelFileList = convertFileDOListToModelFileList(fileList);
        return modelFileList;
    }

    private void updateFileDO(FileDO fileDO, MultipartFile newFile) {
        fileDO.setFileName(newFile.getOriginalFilename());
        fileDO.setFileSize(newFile.getSize());
        fileDO.setUpdatedAt(Instant.now());
    }

    private void verifyFileOwner(FileDO fileDO) {
        String ownerId = CommonRequestHelper.getUserId();

        if (!fileDO.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("You are not authorized to access this file.");
        }
    }

    private BFPFile convertFileDOToModelFile(FileDO fileDO) {
        return BFPFile.builder()
                .id(fileDO.getId())
                .name(fileDO.getFileName())
                .size(fileDO.getFileSize())
                .contentType(fileDO.getContentType())
                .createdAt(fileDO.getCreatedAt().atOffset(ZoneOffset.UTC))
                .modifiedAt(fileDO.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .build();
    }

    private ListedBFPFile convertFileDOToListedModelFile(FileDO fileDO) {
        return ListedBFPFile.builder()
                .id(fileDO.getId())
                .name(fileDO.getFileName())
                .size(fileDO.getFileSize())
                .build();
    }

    private List<ListedBFPFile> convertFileDOListToModelFileList(List<FileDO> fileDOList) {
        List<ListedBFPFile> files = new ArrayList<>();
        for (FileDO fileDO : fileDOList) {
            files.add(convertFileDOToListedModelFile(fileDO));
        }
        return files;
    }
}
