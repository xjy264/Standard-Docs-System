package cn.datong.standard.storage;

import cn.datong.standard.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {
    private final MinioClient minioClient;
    @Value("${app.minio.bucket:standard-docs}")
    private String bucket;

    @Override
    public StoredObject upload(MultipartFile file, String objectName) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .contentType(file.getContentType())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
            return new StoredObject(bucket, objectName, file.getSize(), file.getContentType());
        } catch (Exception ex) {
            if (isStorageConnectionFailure(ex)) {
                throw new BusinessException("文件上传失败：无法连接 MinIO 存储服务，请检查 9000 端口");
            }
            throw new BusinessException("文件上传失败：" + ex.getMessage());
        }
    }

    @Override
    public InputStream download(String bucket, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception ex) {
            throw new BusinessException("文件下载失败：" + ex.getMessage());
        }
    }

    @Override
    public void remove(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception ex) {
            throw new BusinessException("文件删除失败：" + ex.getMessage());
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private boolean isStorageConnectionFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof UnknownHostException
                    || current instanceof NoRouteToHostException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("Connection refused") || message.contains("Failed to connect"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
