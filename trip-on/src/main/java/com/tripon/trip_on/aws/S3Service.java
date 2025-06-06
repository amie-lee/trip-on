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
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
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

    public String uploadFile(MultipartFile multipartFile, String dirPath) throws IOException {
        // 원본 파일 이름과 확장자 분리
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // S3에 저장할 키: 예) "profiles/UUIDxxxxxx.png"
        String key = dirPath + "/" + UUID.randomUUID() + extension;

        // PUT 요청 만들 때, ACL 설정을 제거
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(multipartFile.getContentType())
                .build();

        // 실제 업로드
        s3.putObject(por, RequestBody.fromBytes(multipartFile.getBytes()));

        // 업로드한 후 URL 반환 (버킷이 버킷 정책으로 public-read 권한을 따로 허용했을 경우)
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                regionId,
                key);
    }

    // 필요하다면 기존 파일 삭제용
    public void deleteFile(String key) {
        s3.deleteObject(builder -> builder.bucket(bucketName).key(key).build());
    }
}
