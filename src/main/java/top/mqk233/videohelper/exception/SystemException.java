package top.mqk233.videohelper.exception;

/**
 * 系统异常
 *
 * @author mqk233
 * @since 2021-7-31
 */
public class SystemException extends RuntimeException {
    private static final long serialVersionUID = -6418609940112476511L;

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
