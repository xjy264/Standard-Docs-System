package cn.datong.standard.storage;

public record StoredObject(String bucket, String objectName, long size, String contentType) {
}
