package top.mqk233.videohelper.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import top.mqk233.videohelper.util.ChromiumUtils;

/**
 * RestTemplate配置
 *
 * @author mqk233
 * @since 2021-7-30
 */
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.customizers(restTemplate -> {
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            restTemplate.getInterceptors().add((httpRequest, bytes, clientHttpRequestExecution) -> {
                httpRequest.getHeaders().add(HttpHeaders.USER_AGENT, ChromiumUtils.randomUserAgent());
                return clientHttpRequestExecution.execute(httpRequest, bytes);
            });
        }).build();
    }
}
