package top.mqk233.videohelper.exception;

/**
 * 服务异常
 *
 * @author mqk233
 * @since 2021-7-31
 */
public class ServiceException extends RuntimeException {
    private static final long serialVersionUID = 1372702723339192860L;

    public ServiceException(String message) {
        super(message);
    }
}
