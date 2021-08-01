package top.mqk233.videohelper.config;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import top.mqk233.videohelper.util.ChromiumUtils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * RestTemplate配置
 *
 * @author mqk233
 * @since 2021-7-30
 */
@Configuration
public class RestTemplateConfig {
    private static final Random secureRandom = new SecureRandom();

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.customizers(restTemplate -> {
            List<JSONObject> releases = ChromiumUtils.randomReleases(restTemplate);
            String version = CollectionUtils.isEmpty(releases) ? "92.0.4515.107" : releases.get(secureRandom.nextInt(releases.size())).getString("version");
            String agent = String.format("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36", version);
            restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
            restTemplate.getInterceptors().add((httpRequest, bytes, clientHttpRequestExecution) -> {
                httpRequest.getHeaders().add(HttpHeaders.USER_AGENT, agent);
                return clientHttpRequestExecution.execute(httpRequest, bytes);
            });
        }).build();
    }
}
