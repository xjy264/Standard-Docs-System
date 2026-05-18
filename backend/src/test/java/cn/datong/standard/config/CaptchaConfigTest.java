package cn.datong.standard.config;

import cloud.tianai.captcha.cache.CacheStore;
import cloud.tianai.captcha.cache.impl.LocalCacheStore;
import cloud.tianai.captcha.resource.ResourceProviders;
import cloud.tianai.captcha.resource.ImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.ResourceStore;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import cloud.tianai.captcha.resource.impl.DefaultImageCaptchaResourceManager;
import cloud.tianai.captcha.resource.impl.LocalMemoryResourceStore;
import cloud.tianai.captcha.spring.autoconfiguration.CacheStoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CaptchaConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheStoreAutoConfiguration.class))
            .withUserConfiguration(CaptchaConfig.class, MockRedisConfiguration.class);

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

    @Test
    void captchaConfigPrefersLocalStoreEvenWhenRedisTemplateExists() {
        contextRunner.run((context) -> {
            assertThat(context.getBean(CacheStore.class)).isInstanceOf(LocalCacheStore.class);
            assertThat(context.getBean(ResourceStore.class)).isInstanceOf(LocalMemoryResourceStore.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MockRedisConfiguration {
        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        ImageCaptchaResourceManager imageCaptchaResourceManager(ResourceStore resourceStore) {
            return new DefaultImageCaptchaResourceManager(resourceStore, new ResourceProviders());
        }
    }
}
