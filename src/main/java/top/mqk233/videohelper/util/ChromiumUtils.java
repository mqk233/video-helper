package top.mqk233.videohelper.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * Chromium工具类
 *
 * @author mqk233
 * @since 2021-8-1
 */
public class ChromiumUtils {
    private static final String[] CHANNELS = {"Stable", "Beta", "Dev", "Canary"};

    private static final Random SECURE_RANDOM;

    private static final RestTemplate REST_TEMPLATE;

    static {
        SECURE_RANDOM = new SecureRandom();
        HttpRoutePlanner httpRoutePlanner = new DefaultProxyRoutePlanner(new HttpHost("127.0.0.1", 1080));
        HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(httpRoutePlanner).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        REST_TEMPLATE = new RestTemplate(requestFactory);
    }

    /**
     * 根据chromium的release信息随机userAgent
     *
     * @return userAgent
     */
    public static String randomUserAgent() {
        UserAgent agent = UserAgent.values()[SECURE_RANDOM.nextInt(UserAgent.values().length)];
        String fetchReleasesUrl = String.format("https://chromiumdash.appspot.com/fetch_releases?channel=%s&platform=%s&num=10&offset=0",
                CHANNELS[SECURE_RANDOM.nextInt(CHANNELS.length)], agent.getName());
        ResponseEntity<String> response = REST_TEMPLATE.getForEntity(fetchReleasesUrl, String.class);
        if (response.getStatusCodeValue() == HttpStatus.OK.value()) {
            Optional.ofNullable(response.getBody())
                    .map(a -> JSON.parseArray(a, JSONObject.class))
                    .filter(b -> !CollectionUtils.isEmpty(b))
                    .ifPresent(c -> agent.version = c.get(SECURE_RANDOM.nextInt(c.size())).getString("version"));
        }
        return String.format("Mozilla/5.0 (%s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/%s Safari/537.36", agent.getPlatform(), agent.getVersion());
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
         * 通过系统变量获取枚举
         *
         * @return UserAgent
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
