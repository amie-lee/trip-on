package com.tripon.trip_on.aws;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private final S3Client s3;
    private final String bucketName;
    private final String regionId;

    public S3Service(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey,
            @Value("${aws.s3.bucket-name}") String bucketName) {

        this.regionId = region;
        this.bucketName = bucketName;

        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    // 파일 업로드 - 디렉토리명 지정
    public String uploadFile(MultipartFile multipartFile, String dirPath) throws IOException {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String key = dirPath + "/" + UUID.randomUUID() + extension;

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(multipartFile.getContentType())
                .build();

        s3.putObject(por, RequestBody.fromBytes(multipartFile.getBytes()));

        return key;
    }

    // 파일 업로드 - 랜덤 파일명 생성 (디폴트 dir 없음)
    public String uploadFile(MultipartFile file) throws IOException {
        String extension = "";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = "uploads/" + UUID.randomUUID() + extension;

        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3.putObject(por, RequestBody.fromBytes(file.getBytes()));

        // key만 반환하고 URL은 Controller에서 생성하도록 수정
        return key;
    }

    // S3 URL로부터 파일 삭제
    public void deleteFileByUrl(String imageUrl) {
        String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + 14 + bucketName.length());
        if (key.startsWith("/")) key = key.substring(1);
        final String finalKey = key;
        s3.deleteObject(builder -> builder.bucket(bucketName).key(finalKey).build());
    }
}
