package top.mqk233.videohelper.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mqk233.videohelper.VO.VideoDetailVO;
import top.mqk233.videohelper.VO.VideoSearchVO;
import top.mqk233.videohelper.result.Result;
import top.mqk233.videohelper.result.ResultEnum;
import top.mqk233.videohelper.service.VideoService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 视频控制层
 *
 * @author mqk233
 * @since 2021-7-28
 */
@RestController
@RequestMapping("/video")
@CrossOrigin
public class VideoController {
    @Resource
    private VideoService videoService;

    /**
     * 搜索接口
     *
     * @param keywords 关键词
     * @return 视频数据
     */
    @GetMapping("/search")
    public Result<List<VideoSearchVO>> search(String keywords) {
        if (StringUtils.hasText(keywords)) {
            return new Result<>(ResultEnum.SUCCESS, videoService.search(keywords));
        }
        return new Result<>(ResultEnum.PARAMETER_EXCEPTION);
    }

    /**
     * 详情接口
     *
     * @param address 详情地址
     * @return 视频数据
     */
    @GetMapping("/detail")
    public Result<VideoDetailVO> detail(String address) {
        if (StringUtils.hasText(address)) {
            return new Result<>(ResultEnum.SUCCESS, videoService.detail(address));
        }
        return new Result<>(ResultEnum.PARAMETER_EXCEPTION);
    }
}
