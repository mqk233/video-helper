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
public enum VideoTypeEnum {
    TENCENT("腾讯视频", "v.qq.com"),
    IQIYI("爱奇艺", "iqiyi.com"),
    MANGO("芒果视频", "mgtv.com");

    private String name;

    private String domain;

    public static VideoTypeEnum matchEnumByDomain(String domain) {
        return Arrays.stream(VideoTypeEnum.values())
                .filter(item -> domain.contains(item.getDomain()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(String.format("Unable to match the video type by url: %s", domain)));
    }
}
