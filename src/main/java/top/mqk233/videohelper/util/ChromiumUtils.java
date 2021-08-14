package top.mqk233.videohelper.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * Chromium工具类
 *
 * @author mqk233
 * @since 2021-8-14
 */
@Slf4j
public class ChromiumUtils {
    private static final String[] CHANNELS = {"Stable", "Beta", "Dev", "Canary"};

    private static final Random SECURE_RANDOM = new SecureRandom();

    private static final RestTemplate REST_TEMPLATE = new RestTemplate(new HttpComponentsClientHttpRequestFactory(
            HttpClientBuilder.create().setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("127.0.0.1", 1080))).build()));

    /**
     * 根据chromium的release信息随机userAgent
     *
     * @param random 随机或者根据当前系统生成UserAgent
     * @return UserAgent字符串
     */
    public static String getUserAgent(boolean random) {
        UserAgent userAgent = random ? UserAgent.values()[SECURE_RANDOM.nextInt(UserAgent.values().length)] : UserAgent.getInstance();
        try {
            String channel = CHANNELS[SECURE_RANDOM.nextInt(CHANNELS.length)];
            String url = String.format("https://chromiumdash.appspot.com/fetch_releases?channel=%s&platform=%s&num=10&offset=0", channel, userAgent.getName());
            ResponseEntity<String> response = REST_TEMPLATE.getForEntity(url, String.class);
            if (response.getStatusCodeValue() == HttpStatus.OK.value()) {
                Optional.ofNullable(response.getBody())
                        .map(a -> JSON.parseArray(a, JSONObject.class))
                        .filter(b -> !CollectionUtils.isEmpty(b))
                        .ifPresent(c -> userAgent.version = c.get(SECURE_RANDOM.nextInt(c.size())).getString("version"));
            }
        } catch (RestClientException e) {
            log.error("Failed to fetch chromium releases data for user agent and use default version.");
        }
        return String.format("Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36", userAgent.getPlatform(), userAgent.getVersion());
    }

    @AllArgsConstructor
    @Getter
    private enum UserAgent {
        WINDOWS("Windows", "Windows NT 10.0; Win64; x64", "92.0.4515.107"),
        MAC("Mac", "Macintosh; Intel Mac OS X 10_15_7", "92.0.4515.107"),
        LINUX("Linux", "X11; Linux x86_64", "92.0.4515.107");

        private String name;

        private String platform;

        private String version;

        /**
         * 通过系统变量获取UserAgent(默认Windows平台)
         *
         * @return UserAgent枚举
         */
        public static UserAgent getInstance() {
            return Arrays.stream(UserAgent.values())
                    .filter(a -> Optional.ofNullable(System.getProperty("os.name"))
                            .map(b -> b.toLowerCase().contains(a.getName().toLowerCase()))
                            .orElse(false))
                    .findFirst()
                    .orElse(WINDOWS);
        }
    }
}
