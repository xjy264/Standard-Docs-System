package cn.datong.standard.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    StoredObject upload(MultipartFile file, String objectName);

    InputStream download(String bucket, String objectName);

    void remove(String bucket, String objectName);
}
