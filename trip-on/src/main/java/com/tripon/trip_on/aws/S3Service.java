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

    /**
     * 리뷰 업로드용 (default dir="uploads/"), 키만 반환
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 확장자 추출
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                     ? original.substring(original.lastIndexOf('.'))
                     : "";

        // 키 생성
        String key = "uploads/" + UUID.randomUUID() + ext;

        // S3에 업로드
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3.putObject(por, RequestBody.fromBytes(file.getBytes()));

        return key;
    }

    /**
     * 프로필 업로드용 (dirPath 지정), 전체 퍼블릭 URL을 반환
     */
    public String uploadFile(MultipartFile file, String dirPath) throws IOException {
        // 확장자 추출
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                     ? original.substring(original.lastIndexOf('.'))
                     : "";

        // 키 생성 (예: "profiles/UUID.png")
        String key = dirPath + "/" + UUID.randomUUID() + ext;

        // S3에 업로드
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3.putObject(por, RequestBody.fromBytes(file.getBytes()));

        // 전체 URL 조립
        return String.format(
            "https://%s.s3.%s.amazonaws.com/%s",
            bucketName, regionId, key
        );
    }

    /**
     * 키로 삭제
     */
    public void deleteFile(String key) {
        s3.deleteObject(b -> b.bucket(bucketName).key(key).build());
    }

    /**
     * URL로 삭제
     */
    public void deleteFileByUrl(String imageUrl) {
        // https://{bucket}.s3.{region}.amazonaws.com/{key} 에서 key만
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, regionId);
        String key = imageUrl.startsWith(prefix)
                   ? imageUrl.substring(prefix.length())
                   : imageUrl;  // (fallback)
        s3.deleteObject(b -> b.bucket(bucketName).key(key).build());
    }
}
