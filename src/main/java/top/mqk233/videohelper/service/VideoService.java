package top.mqk233.videohelper.service;

import top.mqk233.videohelper.VO.VideoDetailVO;
import top.mqk233.videohelper.VO.VideoSearchVO;

import java.util.List;

/**
 * 视频接口
 *
 * @author mqk233
 * @since 2021-7-28
 */
public interface VideoService {
    /**
     * 搜索接口
     *
     * @param keywords 关键词
     * @return 视频数据
     */
    List<VideoSearchVO> search(String keywords);

    /**
     * 详情接口
     *
     * @param address 详情地址
     * @return 视频数据
     */
    VideoDetailVO detail(String address);
}
