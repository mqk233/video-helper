package top.mqk233.videohelper.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.mqk233.videohelper.result.Result;
import top.mqk233.videohelper.result.ResultEnum;

/**
 * 全局异常处理
 *
 * @author mqk233
 * @since 2021-7-31
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 系统异常
     *
     * @param systemException 系统异常
     * @return 接口数据
     */
    @ExceptionHandler(value = SystemException.class)
    public Result<String> systemExceptionHandler(SystemException systemException) {
        systemException.getCause().printStackTrace();
        return new Result<>(ResultEnum.SYSTEM_EXCEPTION);
    }

    /**
     * 服务异常
     *
     * @param serviceException 服务异常
     * @return 接口数据
     */
    @ExceptionHandler(value = ServiceException.class)
    public Result<String> serviceExceptionHandler(ServiceException serviceException) {
        log.error(serviceException.getMessage());
        return new Result<>(ResultEnum.SERVICE_EXCEPTION.getCode(), serviceException.getMessage());
    }
}
