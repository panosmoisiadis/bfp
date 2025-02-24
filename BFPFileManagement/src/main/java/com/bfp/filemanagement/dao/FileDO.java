package com.bfp.filemanagement.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "file_table", schema = "bfp")
public class FileDO {
    @Id
    private UUID id;
    @Column(name = "owner_id")
    private String ownerId;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_location")
    private String fileLocation;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "content_type")
    private String contentType;
}
