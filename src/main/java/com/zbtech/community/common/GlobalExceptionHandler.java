package com.zbtech.community.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：统一返回 { code, message }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("default message")) {
            int i = msg.indexOf("default message");
            msg = msg.substring(i).replaceAll("default message \\[|\\]", "").split(";")[0];
        }
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e) {
        return Result.error(ErrorCode.FAIL.getCode(), e.getMessage());
    }

    /** 鉴权/权限异常可由具体拦截器抛出 BizException(UNAUTHORIZED/FORBIDDEN) */
    @ExceptionHandler(jakarta.servlet.ServletException.class)
    public Result<Void> handleServlet(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        if (status != null && status == 401) {
            return Result.error(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
        }
        if (status != null && status == 403) {
            return Result.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
        }
        return Result.error(ErrorCode.FAIL.getCode(), "服务器异常");
    }
}
