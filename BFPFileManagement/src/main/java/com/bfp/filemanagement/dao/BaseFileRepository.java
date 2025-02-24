package com.bfp.filemanagement.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BaseFileRepository extends PagingAndSortingRepository<FileDO, UUID> {
    List<FileDO> findByOwnerId(String ownerId);
    List<FileDO> findByOwnerId(String ownerId, Pageable pageable);
    Optional<FileDO> findById(UUID fileId);
    FileDO save(FileDO fileDO);
    void deleteById(UUID fileId);
    void delete(FileDO fileDO);
    void deleteByOwnerId(String ownerId);
}
