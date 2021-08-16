package top.mqk233.videohelper.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import top.mqk233.videohelper.VO.VideoDetailVO;
import top.mqk233.videohelper.VO.VideoSearchVO;
import top.mqk233.videohelper.constant.VideoSourceEnum;
import top.mqk233.videohelper.exception.ServiceException;
import top.mqk233.videohelper.exception.SystemException;
import top.mqk233.videohelper.service.VideoService;
import top.mqk233.videohelper.util.ChromiumUtils;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
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
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    @Resource
    private RestTemplate restTemplate;

    @Override
    public List<VideoSearchVO> search(String keywords) {
        Future<List<VideoSearchVO>> tencentFuture = THREAD_POOL.submit(() -> tencentSearch(keywords));
        Future<List<VideoSearchVO>> iqiyiFuture = THREAD_POOL.submit(() -> iqiyiSearch(keywords));
        Future<List<VideoSearchVO>> mangoFuture = THREAD_POOL.submit(() -> mangoSearch(keywords));
        Future<List<VideoSearchVO>> youkuFuture = THREAD_POOL.submit(() -> youkuSearch(keywords));
        return Stream.of(
                callbackFuture(keywords, VideoSourceEnum.TENCENT.getId(), tencentFuture),
                callbackFuture(keywords, VideoSourceEnum.IQIYI.getId(), iqiyiFuture),
                callbackFuture(keywords, VideoSourceEnum.MANGO.getId(), mangoFuture),
                callbackFuture(keywords, VideoSourceEnum.YOUKU.getId(), youkuFuture)
        ).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<VideoSearchVO> callbackFuture(String keywords, String source, Future<List<VideoSearchVO>> future) {
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Searching {} videos by keywords: {} timed out.", source, keywords);
        }
        return new ArrayList<>();
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
                            .filter(d -> !d.isEmpty())
                            .map(e -> e.getJSONObject(0))
                            .filter(f -> f.getIntValue("dataType") == 2
                                    && f.getIntValue("videoType") == 2
                                    && Optional.ofNullable(f.getJSONArray("titleMarkLabelList")).map(x1 -> !x1.isEmpty()).orElse(false)
                                    && Optional.ofNullable(f.getJSONArray("videoSrcName")).map(x1 -> !x1.isEmpty()).orElse(false)
                                    && StringUtils.hasText(f.getString("actor"))
                                    && Optional.ofNullable(f.getString("area")).map(x1 -> Arrays.asList("内地", "美国", "英国", "韩国", "泰国", "日本", "中国香港", "中国台湾").contains(x1)).orElse(false)
                                    && Optional.ofNullable(f.getJSONArray("videoSrcName")).map(x1 -> x1.getJSONObject(0)).map(x2 -> x2.getIntValue("displayType") == 0).orElse(false))
                            .map(g -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(g.getJSONArray("videoSrcName"))
                                        .filter(x1 -> !x1.isEmpty())
                                        .map(x2 -> x2.getJSONObject(0))
                                        .map(x3 -> x3.getString("srcName"))
                                        .ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(g.getString("id"))
                                        .map(x1 -> String.format("https://v.qq.com/detail/m/%s.html", x1))
                                        .ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(g.getString("title"))
                                        .map(x1 -> x1.replace("\u0005", "").replace("\u0006", ""))
                                        .ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(g.getString("posterPic"))
                                        .map(x1 -> x1.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(g.getString("actor"))
                                        .map(x1 -> x1.replace("\u0005", "").replace("\u0006", ""))
                                        .map(x2 -> x2.split(" "))
                                        .map(Arrays::asList)
                                        .ifPresent(videoSearchVO::setActors);
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException(String.format("Failed to parse the response of searching %s videos.", VideoSourceEnum.TENCENT.getId())));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by keywords: %s.", VideoSourceEnum.TENCENT.getId(), keywords), e);
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
                                Optional.ofNullable(f.getString("siteId"))
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
                    .orElseThrow(() -> new ServiceException(String.format("Failed to parse the response of searching %s videos.", VideoSourceEnum.IQIYI.getId())));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by keywords: %s.", VideoSourceEnum.IQIYI.getId(), keywords), e);
        }
    }

    private List<VideoSearchVO> mangoSearch(String keywords) {
        try {
            String response = restTemplate.getForObject(new URI("https://mobileso.bz.mgtv.com/msite/search/v2?ty=2&q=" + keywords.trim()), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONObject("data"))
                    .map(b -> b.getJSONArray("contents"))
                    .map(c -> c.stream()
                            .map(String::valueOf)
                            .map(JSON::parseObject)
                            .filter(d -> d.getString("type").equals("media"))
                            .map(e -> e.getJSONArray("data"))
                            .filter(f -> !f.isEmpty())
                            .map(g -> g.getJSONObject(0))
                            .filter(h -> Optional.ofNullable(h.getString("source")).map(x1 -> x1.equals("imgo")).orElse(false)
                                    && Optional.ofNullable(h.getJSONArray("desc")).map(x1 -> x1.size() > 1).orElse(false))
                            .map(i -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(i.getString("source"))
                                        .ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(i.getString("url"))
                                        .map(x1 -> "https://www.mgtv.com" + x1)
                                        .ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(i.getString("title"))
                                        .map(x1 -> x1.replace("<B>", "").replace("</B>", ""))
                                        .ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(i.getString("img"))
                                        .map(x1 -> x1.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(i.getJSONArray("desc"))
                                        .map(x1 -> x1.stream()
                                                .map(String::valueOf)
                                                .filter(x2 -> x2.contains("主演:"))
                                                .findFirst()
                                                .orElse(""))
                                        .map(x3 -> x3.split(":")[1])
                                        .map(x4 -> x4.split(" "))
                                        .map(Arrays::asList)
                                        .map(x5 -> x5.stream()
                                                .filter(StringUtils::hasText)
                                                .collect(Collectors.toList()))
                                        .ifPresent(videoSearchVO::setActors);
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException(String.format("Failed to parse the response of searching %s videos.", VideoSourceEnum.MANGO.getId())));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by keywords: %s.", VideoSourceEnum.MANGO.getId(), keywords), e);
        }
    }

    private List<VideoSearchVO> youkuSearch(String keywords) {
        try {
            String response = restTemplate.getForObject(new URI("https://search.youku.com/api/search?categories=97&keyword=" + keywords.trim()), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONArray("pageComponentList"))
                    .map(b -> b.stream()
                            .map(String::valueOf)
                            .map(JSON::parseObject)
                            .filter(c -> c.containsKey("commonData"))
                            .map(d -> d.getJSONObject("commonData"))
                            .filter(e -> e.getIntValue("isYouku") == 1
                                    && e.getIntValue("hasYouku") == 1
                                    && Optional.ofNullable(e.getString("feature")).map(x1 -> x1.contains("电视剧")).orElse(false))
                            .map(f -> {
                                VideoSearchVO videoSearchVO = new VideoSearchVO();
                                Optional.ofNullable(f.getString("sourceName"))
                                        .map(x1 -> VideoSourceEnum.getEnumByName(x1).getId())
                                        .ifPresent(videoSearchVO::setSource);
                                Optional.ofNullable(f.getJSONObject("leftButtonDTO"))
                                        .map(x1 -> x1.getJSONObject("action"))
                                        .map(x2 -> x2.getString("value"))
                                        .ifPresent(videoSearchVO::setAddress);
                                Optional.ofNullable(f.getJSONObject("titleDTO"))
                                        .map(x1 -> x1.getString("displayName"))
                                        .ifPresent(videoSearchVO::setName);
                                Optional.ofNullable(f.getJSONObject("posterDTO"))
                                        .map(x1 -> x1.getString("vThumbUrl"))
                                        .map(x2 -> x2.replace("http", "https"))
                                        .ifPresent(videoSearchVO::setCover);
                                Optional.ofNullable(f.getString("director"))
                                        .map(x1 -> x1.split("：")[1])
                                        .map(x2 -> x2.split(" "))
                                        .map(Arrays::asList)
                                        .ifPresent(videoSearchVO::setActors);
                                return videoSearchVO;
                            }).collect(Collectors.toList()))
                    .orElseThrow(() -> new ServiceException(String.format("Failed to parse the response of searching %s videos.", VideoSourceEnum.YOUKU.getId())));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by keywords: %s.", VideoSourceEnum.YOUKU.getId(), keywords), e);
        }
    }

    @Override
    public VideoDetailVO detail(String address) {
        switch (VideoSourceEnum.matchEnumByDomain(address)) {
            case TENCENT:
                return tencentDetail(address);
            case IQIYI:
                return iqiyiDetail(address);
            case MANGO:
                return mangoDetail(address);
            case YOUKU:
                return youkuDetail(address);
        }
        throw new ServiceException(String.format("Unable to get the video details by url: %s", address));
    }

    private VideoDetailVO tencentDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.getUserAgent(true)).get();
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
            throw new SystemException(String.format("Failed to search %s videos by url: %s.", VideoSourceEnum.TENCENT.getId(), address), e);
        }
    }

    private VideoDetailVO iqiyiDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.getUserAgent(true)).get();
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
            throw new SystemException(String.format("Failed to search %s videos by url: %s.", VideoSourceEnum.IQIYI.getId(), address), e);
        }
    }

    private VideoDetailVO mangoDetail(String address) {
        try {
            String vid = address.substring(address.lastIndexOf("/") + 1, address.lastIndexOf("."));
            String response = restTemplate.getForObject(new URI("https://pcweb.api.mgtv.com/video/info?vid=" + vid), String.class);
            return Optional.ofNullable(response)
                    .map(JSON::parseObject)
                    .map(a -> a.getJSONObject("data"))
                    .map(b -> b.getJSONObject("info"))
                    .map(c -> {
                        VideoDetailVO videoDetailVO = new VideoDetailVO();
                        Optional.ofNullable(c.getString("clipName"))
                                .ifPresent(videoDetailVO::setName);
                        Optional.ofNullable(c.getJSONObject("detail"))
                                .map(x1 -> x1.getString("story"))
                                .ifPresent(videoDetailVO::setDescription);
                        try {
                            String response2 = restTemplate.getForObject(new URI("https://pcweb.api.mgtv.com/episode/list?video_id=" + vid), String.class);
                            Optional.ofNullable(response2)
                                    .map(JSON::parseObject)
                                    .map(d -> d.getJSONObject("data"))
                                    .map(e -> e.getJSONArray("list"))
                                    .map(f -> f.stream()
                                            .map(String::valueOf)
                                            .map(JSON::parseObject)
                                            .filter(g -> Optional.ofNullable(g.getString("isIntact")).map(x1 -> x1.equals("1")).orElse(false))
                                            .collect(Collectors.toMap(
                                                    h -> Optional.ofNullable(h.getString("t1"))
                                                            .orElse("0"),
                                                    h -> Optional.ofNullable(h.getString("url"))
                                                            .map(x1 -> "https://www.mgtv.com" + x1)
                                                            .orElse(""),
                                                    (x1, x2) -> x1,
                                                    () -> new TreeMap<>((x1, x2) -> StringUtil.isNumeric(x1) && StringUtil.isNumeric(x2) ? 1 : x1.compareTo(x2)))))
                                    .ifPresent(videoDetailVO::setEpisodes);
                        } catch (Exception e) {
                            throw new SystemException(String.format("Failed to search %s videos by url: %s.", VideoSourceEnum.MANGO.getId(), address), e);
                        }
                        return videoDetailVO;
                    }).orElseThrow(() -> new ServiceException(String.format("Failed to parse the response of searching %s videos.", VideoSourceEnum.MANGO.getId())));
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by url: %s.", VideoSourceEnum.MANGO.getId(), address), e);
        }
    }

    private VideoDetailVO youkuDetail(String address) {
        try {
            Document document = Jsoup.connect(address).userAgent(ChromiumUtils.getUserAgent(true)).header("authority", "v.youku.com").get();
            VideoDetailVO videoDetailVO = new VideoDetailVO();
            Optional.ofNullable(document.selectFirst("a.title-link"))
                    .map(Element::text)
                    .ifPresent(videoDetailVO::setName);
            Optional.ofNullable(document.selectFirst("div.info"))
                    .map(Element::text)
                    .ifPresent(videoDetailVO::setDescription);
            Optional.ofNullable(document.selectFirst("div.anthology-content"))
                    .map(Element::children)
                    .map(x1 -> x1.stream()
                            .collect(Collectors.toMap(
                                    x2 -> Optional.ofNullable(x2)
                                            .map(x3 -> x3.attr("data-spm"))
                                            .map(z -> z.substring(z.lastIndexOf("_") + 1))
                                            .orElse("0"),
                                    x4 -> Optional.ofNullable(x4)
                                            .map(x5 -> x5.attr("href"))
                                            .map(x6 -> x6.replace("http", "https"))
                                            .orElse(""),
                                    (x7, x8) -> x7,
                                    () -> new TreeMap<>((x9, x10) -> StringUtil.isNumeric(x9) && StringUtil.isNumeric(x10) ? 1 : x9.compareTo(x10)))))
                    .ifPresent(videoDetailVO::setEpisodes);
            return videoDetailVO;
        } catch (Exception e) {
            throw new SystemException(String.format("Failed to search %s videos by url: %s.", VideoSourceEnum.YOUKU.getId(), address), e);
        }
    }
}
