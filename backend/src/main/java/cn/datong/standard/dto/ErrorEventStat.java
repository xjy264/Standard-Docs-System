package cn.datong.standard.dto;

public record ErrorEventStat(
        String fingerprint,
        String source,
        String severity,
        String message,
        long count
) {
}
