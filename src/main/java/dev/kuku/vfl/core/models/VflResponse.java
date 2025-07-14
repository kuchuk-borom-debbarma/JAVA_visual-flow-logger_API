package dev.kuku.vfl.core.models;

public class VflResponse<T> {
    private String message;
    private T data;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return String.format("VflResponse [message=%s, data=%s]", message, data == null ? "null" : data.toString());
    }
}
