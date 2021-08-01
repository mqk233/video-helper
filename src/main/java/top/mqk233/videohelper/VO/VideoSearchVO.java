package top.mqk233.videohelper.VO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 视频搜索数据
 *
 * @author mqk233
 * @since 2021-7-30
 */
@Setter
@Getter
public class VideoSearchVO {
    /**
     * 视频来源
     */
    private String source;
    /**
     * 视频地址
     */
    private String address;
    /**
     * 视频名称
     */
    private String name;
    /**
     * 视频封面
     */
    private String cover;
    /**
     * 视频主演
     */
    private List<String> actors;
}
