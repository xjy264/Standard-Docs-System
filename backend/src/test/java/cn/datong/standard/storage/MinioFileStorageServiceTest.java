package cn.datong.standard.storage;

import cn.datong.standard.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioFileStorageServiceTest {

    @Test
    void uploadReportsClearMessageWhenMinioIsUnavailable() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new ConnectException("Connection refused"));
        MinioFileStorageService service = new MinioFileStorageService(minioClient);
        ReflectionTestUtils.setField(service, "bucket", "standard-docs");
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.upload(file, "docs/a.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件上传失败：无法连接 MinIO 存储服务，请检查 9000 端口");
    }
}
