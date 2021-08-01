package top.mqk233.videohelper.VO;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 视频详情数据
 *
 * @author mqk233
 * @since 2021-7-30
 */
@Setter
@Getter
public class VideoDetailVO {
    /**
     * 视频名称
     */
    private String name;
    /**
     * 视频描述
     */
    private String description;
    /**
     * 视频集数
     */
    private Map<String, String> episodes;
}
