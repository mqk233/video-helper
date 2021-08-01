package top.mqk233.videohelper.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Chromium工具类
 *
 * @author mqk233
 * @since 2021-8-1
 */
public class ChromiumUtils {
    private static final String[] channels = {"Stable", "Beta", "Dev", "Canary"};

    private static final String[] platforms = {"Windows", "Mac", "Linux"};

    private static final Random secureRandom = new SecureRandom();

    public static List<JSONObject> randomReleases(RestTemplate restTemplate) {
        HttpRoutePlanner httpRoutePlanner = new DefaultProxyRoutePlanner(new HttpHost("127.0.0.1", 1080));
        HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(httpRoutePlanner).build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        String channel = channels[secureRandom.nextInt(channels.length)];
        String platform = Optional.ofNullable(System.getProperty("sun.desktop")).map(StringUtils::capitalize).orElse(platforms[2]);
        String fetchReleasesUrl = String.format("https://chromiumdash.appspot.com/fetch_releases?channel=%s&platform=%s&num=10&offset=0", channel, platform);
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(fetchReleasesUrl, String.class);
        return responseEntity.getStatusCodeValue() == HttpStatus.OK.value()
                ? Optional.ofNullable(responseEntity.getBody()).map(item -> JSON.parseArray(item, JSONObject.class)).orElse(new ArrayList<>())
                : new ArrayList<>();
    }
}
