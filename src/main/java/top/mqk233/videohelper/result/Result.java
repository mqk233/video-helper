package top.mqk233.videohelper.result;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一接口返回数据
 *
 * @author mqk233
 * @since 2021-7-31
 */
@Setter
@Getter
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(ResultEnum resultEnum) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
    }

    public Result(ResultEnum resultEnum, T data) {
        this.code = resultEnum.getCode();
        this.message = resultEnum.getMessage();
        this.data = data;
    }
}
