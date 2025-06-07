package com.tripon.trip_on.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 리전을 ap-southeast-2로 고정
    private final String region = "ap-southeast-2";

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = createFileName(file.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3Client.putObject(
            new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata)
        );

        return fileName;
    }

    private String createFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }

    public void deleteFileByUrl(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        amazonS3Client.deleteObject(bucket, fileName);
    }
} 