package cn.datong.standard.config;

import cloud.tianai.captcha.resource.ResourceProviders;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaConfigTest {

    @Test
    void captchaResourceInitializerRegistersSliderBackgroundsInWrappedStore() throws Exception {
        LocalMemoryResourceStore store = new LocalMemoryResourceStore();
        store.addResource("SLIDER", new Resource("classpath", "META-INF/cut-image/resource/1.jpg", "default"));
        DefaultImageCaptchaResourceManager resourceManager =
                new DefaultImageCaptchaResourceManager(store, new ResourceProviders());
        ApplicationRunner runner = new CaptchaConfig().captchaResourceInitializer(resourceManager);

        runner.run(null);

        assertThat(store.listResourcesByTypeAndTag("SLIDER", "default")).hasSize(20);
    }
}
