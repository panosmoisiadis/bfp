package com.bfp.filemanagement.dao;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class S3FileDAO implements FileDAO {
    private final S3Client s3Client;
    private final String bucketName;

    public S3FileDAO(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public byte[] getFileBytes(String fileId) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build()).asByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFileSteam(String fileId) {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String createFile(String fileId, MultipartFile file) throws IOException {
        return uploadFileToS3(fileId, file.getInputStream(), file.getSize());
    }


    @Override
    public void deleteFile(String fileId) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileId)
                .build());
    }

    @Override
    public void updateFile(String fileId, MultipartFile file) throws IOException {
        uploadFileToS3(fileId, file.getInputStream(), file.getSize());
    }

    private String uploadFileToS3(String fileName, InputStream inputStream, long contentLength) {
        List<CompletedPart> completedParts = new ArrayList<>();
        int partNumber = 1;

        CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build());
        UploadPartResponse uploadPartResponse = s3Client.uploadPart(UploadPartRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .uploadId(createMultipartUploadResponse.uploadId())
                .partNumber(partNumber)
                .build(), RequestBody.fromInputStream(inputStream, contentLength));
        completedParts.add(CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(uploadPartResponse.eTag())
                .build());
        CompletedMultipartUpload completed = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();
        CompleteMultipartUploadResponse completeMultipartUploadResponse = s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .uploadId(createMultipartUploadResponse.uploadId())
                .multipartUpload(completed)
                .build());
        return completeMultipartUploadResponse.location();
    }
}
