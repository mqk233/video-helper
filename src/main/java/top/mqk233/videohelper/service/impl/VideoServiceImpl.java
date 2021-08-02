package top.mqk233.videohelper.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import top.mqk233.videohelper.VO.VideoDetailVO;
import top.mqk233.videohelper.VO.VideoSearchVO;
import top.mqk233.videohelper.exception.ServiceException;
import top.mqk233.videohelper.exception.SystemException;
import top.mqk233.videohelper.service.VideoService;
import top.mqk233.videohelper.util.ChromiumUtils;
import top.mqk233.videohelper.util.JsonUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 视频服务层
 *
 * @author mqk233
 * @since 2021-7-28
 */
@Service
@Slf4j
public class VideoServiceImpl implements VideoService {
    @Resource
    private RestTemplate restTemplate;

    @Override
    public List<VideoSearchVO> search(String keywords) {
        return Stream.concat(
                        tencentSearch(keywords).stream(),
                        iqiyiSearch(keywords).stream())
                .collect(Collectors.toList());
    }

    private List<VideoSearchVO> tencentSearch(String keywords) {
        try {
            log.info(String.format("Start searching for tencent videos by keywords: %s.", keywords));
            ResponseEntity<String> response =
                    restTemplate.getForEntity(new URI("https://node.video.qq.com/x/api/msearch?filterValue=tabid%3D2&keyWord=" + keywords), String.class);
            log.info(String.format("The response code is %s, start parsing response result.", response.getStatusCodeValue()));
            return Optional.ofNullable(response.getBody())
                    .map(a -> a.replace("\\u0005", "").replace("\\u0006", ""))
                    .map(JSON::parseObject)
                    .map(b -> b.getJSONArray("uiData"))
                    .map(JsonUtils::arrayToList)
                    .map(c -> c.stream()
                            .map(d -> d.getJSONArray("data"))
                            .map(e -> e.getJSONObject(0))
                            .filter(f -> Optional.ofNullable(f)
                                    .map(g -> g.getIntValue("dataType") == 2
                                            && g.getIntValue("videoType") == 2
                                            && StringUtils.hasText(g.getString("actor"))
                                            && Arrays.asList("内地", "美国", "英国", "韩国", "泰国", "日本", "中国香港", "中国台湾").contains(g.getString("area"))
                                            && StringUtils.hasText(g.getString("secondTitle"))
                                            && !g.getJSONArray("titleMarkLabelList").isEmpty()
                                            && !g.getJSONArray("videoSrcName").isEmpty()
                                            && Optional.ofNullable(g.getJSONArray("videoSrcName").getJSONObject(0)).map(x1 -> x1.getIntValue("displayType") == 0).orElse(false))
                                    .orElse(false))
                            .map(h -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(h.getJSONArray("videoSrcName")).map(x1 -> x1.getJSONObject(0)).map(x2 -> x2.getString("name")).ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(h.getString("id")).map(x1 -> String.format("https://v.qq.com/detail/m/%s.html", x1)).ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(h.getString("title")).ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(h.getString("posterPic")).map(x1 -> x1.replace("http", "https")).ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(h.getString("actor")).map(x1 -> x1.split(" ")).map(Arrays::asList).ifPresent(videoSearchVO::setActors);
                                log.info("Successfully parse the response of searching tencent videos.");
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException("Failed to parse the response of searching tencent videos."));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search tencent videos by keywords: %s.", keywords), e);
        }
    }

    private List<VideoSearchVO> iqiyiSearch(String keywords) {
        try {
            log.info(String.format("Start searching for iqiyi videos by keywords: %s.", keywords));
            ResponseEntity<String> response =
                    restTemplate.getForEntity(new URI("https://search.video.iqiyi.com/o?if=html5&channel_name=电视剧&key=" + keywords), String.class);
            log.info(String.format("The response code is %s, start parsing response result.", response.getStatusCodeValue()));
            return Optional.ofNullable(response.getBody())
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONObject("data"))
                    .map(b -> b.getJSONArray("docinfos"))
                    .map(JsonUtils::arrayToList)
                    .map(c -> c.stream()
                            .map(d -> d.getJSONObject("albumDocInfo"))
                            .filter(e -> e.getIntValue("videoDocType") == 1 && e.getString("siteId").equals("iqiyi"))
                            .map(f -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(f.getString("siteName")).ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(f.getString("albumLink")).map(x1 -> x1.replace("http", "https")).ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(f.getString("albumTitle")).ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(f.getString("albumImg")).map(x1 -> x1.replace("http", "https")).ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(f.getString("star")).map(x1 -> x1.split(";")).map(Arrays::asList).ifPresent(videoSearchVO::setActors);
                                log.info("Successfully parse the response of searching iqiyi videos.");
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException("Failed to parse the response of searching iqiyi videos."));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search iqiyi videos by keywords: %s.", keywords), e);
        }
    }

    @Override
    public VideoDetailVO detail(String address) {
        if (address.contains("v.qq.com")) {
            return tencentDetail(address);
        }
        if (address.contains("iqiyi.com")) {
            return iqiyiDetail(address);
        }
        return null;
    }

    private VideoDetailVO tencentDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.randomUserAgent()).get();
            VideoDetailVO videoDetailVO = new VideoDetailVO();
            Optional.ofNullable(document.selectFirst("h1.video_title_cn")).map(x1 -> x1.selectFirst("a")).map(Element::text).ifPresent(videoDetailVO::setName);
            Optional.ofNullable(document.selectFirst("span._desc_txt_lineHight")).map(Element::text).ifPresent(videoDetailVO::setDescription);
            Optional.ofNullable(document.selectFirst("div.mod_episode"))
                    .map(Element::children)
                    .map(x1 -> x1.stream()
                            .filter(x2 -> Optional.ofNullable(x2.selectFirst("img.mark_pic"))
                                    .map(x3 -> x3.attr("src"))
                                    .map(x4 -> !x4.endsWith("tag_mini_trailerlite.png"))
                                    .orElse(true))
                            .map(x5 -> x5.selectFirst("a"))
                            .collect(Collectors.toMap(
                                    x6 -> Optional.ofNullable(x6)
                                            .map(x7 -> x7.selectFirst("span"))
                                            .map(Element::text)
                                            .orElse("0"),
                                    x8 -> Optional.ofNullable(x8)
                                            .map(x9 -> x9.attr("href"))
                                            .orElse(""),
                                    (s1, s2) -> s1,
                                    () -> new TreeMap<>((o1, o2) -> StringUtil.isNumeric(o1) && StringUtil.isNumeric(o2) ? 1 : o1.compareTo(o2)))))
                    .ifPresent(videoDetailVO::setEpisodes);
            return videoDetailVO;
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search tencent videos by url: %s.", address), e);
        }
    }

    private VideoDetailVO iqiyiDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.randomUserAgent()).get();
            VideoDetailVO videoDetailVO = new VideoDetailVO();
            Optional.ofNullable(document.selectFirst("h1.album-head-title")).map(x1 -> x1.selectFirst("a")).map(Element::text).ifPresent(videoDetailVO::setName);
            Optional.ofNullable(Optional.ofNullable(document.selectFirst("div.episodeIntro-brief"))
                            .orElse(document.selectFirst("div.album-head-des")))
                    .map(x1 -> x1.attr("title"))
                    .ifPresent(videoDetailVO::setDescription);
            Optional.ofNullable(document.selectFirst("ul.album-numlist"))
                    .map(Element::children)
                    .map(x1 -> x1.stream()
                            .filter(x2 -> Optional.ofNullable(x2.selectFirst("img"))
                                    .map(x3 -> x3.attr("src"))
                                    .map(x4 -> !x4.endsWith("s-pre.png"))
                                    .orElse(true))
                            .map(x5 -> x5.selectFirst("a"))
                            .collect(Collectors.toMap(
                                    x6 -> Optional.ofNullable(x6)
                                            .map(x7 -> x7.attr("title"))
                                            .orElse("0"),
                                    x8 -> Optional.ofNullable(x8)
                                            .map(x9 -> x9.attr("href"))
                                            .map(x10 -> "https:" + x10)
                                            .orElse(""),
                                    (s1, s2) -> s1,
                                    () -> new TreeMap<>((o1, o2) -> StringUtil.isNumeric(o1) && StringUtil.isNumeric(o2) ? 1 : o1.compareTo(o2)))))
                    .ifPresent(videoDetailVO::setEpisodes);
            return videoDetailVO;
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search iqiyi videos by url: %s.", address), e);
        }
    }

    @Override
    public String resolve(String sourceAddress) {
        try {
            String response = restTemplate.getForObject(new URI("http://admin.vodjx.top/json.php?url=" + sourceAddress), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(x1 -> x1.getString("url"))
                    .orElseThrow(() -> new ServiceException("Failed to parse the response of resolving videos."));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to resolve the video by url: %s.", sourceAddress), e);
        }
    }
}
