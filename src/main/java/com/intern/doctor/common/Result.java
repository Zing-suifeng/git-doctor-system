package com.intern.doctor.common;

/**
 * 统一返回结果
 *
 * @param <T> 数据类型
 */
public class Result<T> {
    private Integer code;   // 200 成功；500 失败
    private String msg;     // 提示信息
    private T data;         // 数据

    private Result() {}

    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        return r;
    }

    public static <T> Result<T> success(String msg) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> error(String msg) {
        return error(500, msg);
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
