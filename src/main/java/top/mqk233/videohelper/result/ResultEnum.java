package top.mqk233.videohelper.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一接口返回数据枚举
 *
 * @author mqk233
 * @since 2021-7-31
 */
@AllArgsConstructor
@Getter
public enum ResultEnum {
    SUCCESS(1000, "operate succeed"),
    PARAMETER_EXCEPTION(1001, "parameter abnormal"),
    SERVICE_EXCEPTION(1002, "service abnormal"),
    SYSTEM_EXCEPTION(1003, "system abnormal");

    private int code;
    private String message;
}
