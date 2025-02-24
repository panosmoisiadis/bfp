package com.bfp.filemanagement.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class PostgresFileMetadataDAO implements FileMetadataDAO {
    @Autowired
    private BaseFileRepository fileRepository;

    @Override
    public Optional<FileDO> getFile(UUID fileId) {
        return fileRepository.findById(fileId);
    }

    @Override
    public FileDO saveFile(FileDO fileDO) {
        return fileRepository.save(fileDO);
    }

    @Override
    public List<FileDO> getAllFilesByOwnerId(String ownerId) {
        return fileRepository.findByOwnerId(ownerId, Pageable.unpaged());
    }

    @Override
    public void deleteFile(UUID fileId) {
        fileRepository.deleteById(fileId);
    }

    @Override
    public void deleteFile(FileDO fileDO) {
        fileRepository.delete(fileDO);
    }

    @Override
    public void updateFile(FileDO fileDO) {
        fileRepository.save(fileDO);
    }

    @Override
    public void deleteAllFilesByOwnerId(String ownerId) {
        fileRepository.deleteByOwnerId(ownerId);
    }
}
