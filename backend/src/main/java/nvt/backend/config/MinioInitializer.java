package nvt.backend.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioInitializer implements CommandLineRunner {

    private final MinioClient minioClient;

    @Value("${minio.bucket.company-images}")
    private String companyImagesBucket;

    @Value("${minio.bucket.company-documents}")
    private String companyDocumentsBucket;

    @Value("${minio.bucket.vehicle-images}")
    private String vehicleImagesBucket;

    @Override
    public void run(String... args) {
        createBucketIfNotExists(companyImagesBucket);
        createBucketIfNotExists(companyDocumentsBucket);
        createBucketIfNotExists(vehicleImagesBucket);
    }

    private void createBucketIfNotExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Created MinIO bucket: {}", bucketName);
            } else {
                log.info("MinIO bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket: {}", bucketName, e);
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucketName, e);
        }
    }
}