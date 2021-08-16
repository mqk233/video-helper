package top.mqk233.videohelper.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.mqk233.videohelper.exception.ServiceException;

import java.util.Arrays;

/**
 * 视频来源枚举
 *
 * @author mqk233
 * @since 2021-8-14
 */
@AllArgsConstructor
@Getter
public enum VideoSourceEnum {
    TENCENT("qq", "腾讯视频", "v.qq.com"),
    IQIYI("iqiyi", "爱奇艺", "iqiyi.com"),
    MANGO("imgo", "芒果TV", "mgtv.com");

    private String id;

    private String name;

    private String domain;

    public static VideoSourceEnum matchEnumByDomain(String address) {
        return Arrays.stream(VideoSourceEnum.values())
                .filter(item -> address.contains(item.getDomain()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(String.format("Unable to match the video source by url: %s", address)));
    }
}
