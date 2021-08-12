package top.mqk233.videohelper.service.impl;

import com.alibaba.fastjson.JSON;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import top.mqk233.videohelper.VO.VideoDetailVO;
import top.mqk233.videohelper.VO.VideoSearchVO;
import top.mqk233.videohelper.constant.VideoConstants;
import top.mqk233.videohelper.exception.ServiceException;
import top.mqk233.videohelper.exception.SystemException;
import top.mqk233.videohelper.service.VideoService;
import top.mqk233.videohelper.util.ChromiumUtils;

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
public class VideoServiceImpl implements VideoService {
    @Resource
    private RestTemplate restTemplate;

    @Override
    public List<VideoSearchVO> search(String keywords) {
        return Stream.concat(tencentSearch(keywords).stream(), iqiyiSearch(keywords).stream()).collect(Collectors.toList());
    }

    private List<VideoSearchVO> tencentSearch(String keywords) {
        try {
            String response = restTemplate.getForObject(new URI("https://node.video.qq.com/x/api/msearch?filterValue=tabid%3D2&keyWord=" + keywords.trim()), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONArray("uiData"))
                    .map(b -> b.stream()
                            .map(String::valueOf)
                            .map(JSON::parseObject)
                            .map(c -> c.getJSONArray("data"))
                            .map(d -> d.getJSONObject(0))
                            .filter(e -> e.getIntValue("dataType") == 2
                                    && e.getIntValue("videoType") == 2
                                    && Optional.ofNullable(e.getJSONArray("titleMarkLabelList")).map(x1 -> !x1.isEmpty()).orElse(false)
                                    && Optional.ofNullable(e.getJSONArray("videoSrcName")).map(x1 -> !x1.isEmpty()).orElse(false)
                                    && StringUtils.hasText(e.getString("actor"))
                                    && Optional.ofNullable(e.getString("area")).map(x1 -> Arrays.asList("内地", "美国", "英国", "韩国", "泰国", "日本", "中国香港", "中国台湾").contains(x1)).orElse(false)
                                    && Optional.ofNullable(e.getJSONArray("videoSrcName")).map(x1 -> x1.getJSONObject(0)).map(x2 -> x2.getIntValue("displayType") == 0).orElse(false))
                            .map(f -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(f.getJSONArray("videoSrcName"))
                                        .map(x1 -> x1.getJSONObject(0))
                                        .map(x2 -> x2.getString("name"))
                                        .map(x3 -> x3.replace("\u0005", "").replace("\u0006", ""))
                                        .ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(f.getString("id"))
                                        .map(x1 -> String.format("https://v.qq.com/detail/m/%s.html", x1))
                                        .ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(f.getString("title"))
                                        .map(x1 -> x1.replace("\u0005", "").replace("\u0006", ""))
                                        .ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(f.getString("posterPic"))
                                        .map(x1 -> x1.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(f.getString("actor"))
                                        .map(x1 -> x1.replace("\u0005", "").replace("\u0006", ""))
                                        .map(x2 -> x2.split(" "))
                                        .map(Arrays::asList)
                                        .ifPresent(videoSearchVO::setActors);
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException("Failed to parse the response of searching tencent videos."));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search tencent videos by keywords: %s.", keywords), e);
        }
    }

    private List<VideoSearchVO> iqiyiSearch(String keywords) {
        try {
            String response = restTemplate.getForObject(new URI("https://search.video.iqiyi.com/o?if=html5&channel_name=电视剧&key=" + keywords.trim()), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONObject("data"))
                    .map(b -> b.getJSONArray("docinfos"))
                    .map(c -> c.stream()
                            .map(String::valueOf)
                            .map(JSON::parseObject)
                            .map(d -> d.getJSONObject("albumDocInfo"))
                            .filter(e -> e.getIntValue("videoDocType") == 1
                                    && Optional.ofNullable(e.getString("siteId")).map(x1 -> x1.equals("iqiyi")).orElse(false))
                            .map(f -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(f.getString("siteName"))
                                        .ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(f.getString("albumLink"))
                                        .map(x1 -> x1.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(f.getString("albumTitle"))
                                        .ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(f.getString("albumImg"))
                                        .map(x1 -> x1.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(f.getString("star"))
                                        .map(x1 -> x1.split(";"))
                                        .map(Arrays::asList)
                                        .ifPresent(videoSearchVO::setActors);
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException("Failed to parse the response of searching iqiyi videos."));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search iqiyi videos by keywords: %s.", keywords), e);
        }
    }

    @Override
    public VideoDetailVO detail(String address) {
        if (address.contains(VideoConstants.TENCENT_DOMAIN)) {
            return tencentDetail(address);
        }
        if (address.contains(VideoConstants.IQIYI_DOMAIN)) {
            return iqiyiDetail(address);
        }
        throw new ServiceException(String.format("Unable to get the video details: %s", address));
    }

    private VideoDetailVO tencentDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.randomUserAgent()).get();
            VideoDetailVO videoDetailVO = new VideoDetailVO();
            Optional.ofNullable(document.selectFirst("h1.video_title_cn"))
                    .map(x1 -> x1.selectFirst("a")).map(Element::text)
                    .ifPresent(videoDetailVO::setName);
            Optional.ofNullable(document.selectFirst("span._desc_txt_lineHight"))
                    .map(Element::text)
                    .ifPresent(videoDetailVO::setDescription);
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
                                    (x10, x11) -> x10,
                                    () -> new TreeMap<>((x12, x13) -> StringUtil.isNumeric(x12) && StringUtil.isNumeric(x13) ? 1 : x12.compareTo(x13)))))
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
            Optional.ofNullable(document.selectFirst("h1.album-head-title"))
                    .map(x1 -> x1.selectFirst("a.title-link"))
                    .map(Element::text)
                    .ifPresent(videoDetailVO::setName);
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
                                            .map(Element::text)
                                            .orElse("0"),
                                    x7 -> Optional.ofNullable(x7)
                                            .map(x8 -> x8.attr("href"))
                                            .map(x9 -> "https:" + x9)
                                            .orElse(""),
                                    (x10, x11) -> x10,
                                    () -> new TreeMap<>((x12, x13) -> StringUtil.isNumeric(x12) && StringUtil.isNumeric(x13) ? 1 : x12.compareTo(x13)))))
                    .ifPresent(videoDetailVO::setEpisodes);
            return videoDetailVO;
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search iqiyi videos by url: %s.", address), e);
        }
    }
}
