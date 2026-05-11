package cn.datong.standard.config;

import cloud.tianai.captcha.resource.CrudResourceStore;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class CaptchaConfig {
    private static final String SLIDER_TYPE = "SLIDER";
    private static final String DEFAULT_TAG = "default";
    private static final String[] CUSTOM_BACKGROUNDS = {
            "captcha/backgrounds/bg-01.jpg",
            "captcha/backgrounds/bg-02.jpg",
            "captcha/backgrounds/bg-03.jpg",
            "captcha/backgrounds/bg-04.jpg",
            "captcha/backgrounds/bg-05.jpg",
            "captcha/backgrounds/bg-06.jpg",
            "captcha/backgrounds/bg-07.jpg",
            "captcha/backgrounds/bg-08.jpg",
            "captcha/backgrounds/bg-09.jpg",
            "captcha/backgrounds/bg-10.jpg",
            "captcha/backgrounds/bg-11.jpg",
            "captcha/backgrounds/bg-12.jpg",
            "captcha/backgrounds/bg-13.jpg",
            "captcha/backgrounds/bg-14.jpg",
            "captcha/backgrounds/bg-15.jpg",
            "captcha/backgrounds/bg-16.jpg",
            "captcha/backgrounds/bg-17.jpg",
            "captcha/backgrounds/bg-18.jpg",
            "captcha/backgrounds/bg-19.jpg",
            "captcha/backgrounds/bg-20.jpg"
    };

    @Bean
    public ApplicationRunner captchaResourceInitializer(ImageCaptchaResourceManager resourceManager) {
        return args -> {
            ResourceStore store = resourceManager.getResourceStore().getTarget();
            if (store instanceof CrudResourceStore crudStore) {
                crudStore.clearAllResources();
                for (String background : CUSTOM_BACKGROUNDS) {
                    crudStore.addResource(SLIDER_TYPE, new Resource("classpath", background, DEFAULT_TAG));
                }
                log.info("滑块验证码背景图资源已加载：{} 张", CUSTOM_BACKGROUNDS.length);
            } else {
                log.warn("滑块验证码资源存储不支持动态注册背景图：{}", store.getClass().getName());
            }
        };
    }
}
