package cn.datong.standard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Data
@Component
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {
    private String provider = "local";

    public boolean isDisabled() {
        return "none".equals(provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT));
    }
}
