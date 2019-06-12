package com.github.rkharisov.chunks.model.response;

public class UnaryResponse implements Response {

    private String value;

    public UnaryResponse() {
    }

    public UnaryResponse(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
