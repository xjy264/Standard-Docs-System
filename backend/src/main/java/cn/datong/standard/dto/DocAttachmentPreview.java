package cn.datong.standard.dto;

public record DocAttachmentPreview(
        String previewType,
        String fileType,
        String title,
        String url,
        String documentServerUrl,
        String message
) {
}
