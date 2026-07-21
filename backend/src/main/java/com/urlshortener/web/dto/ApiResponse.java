package com.urlshortener.web.dto;

import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        PageMeta meta,
        List<String> warnings,
        ErrorBody error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, List.of(), null);
    }

    public static <T> ApiResponse<T> ok(T data, List<String> warnings) {
        return new ApiResponse<>(true, data, null, warnings == null ? List.of() : List.copyOf(warnings), null);
    }

    public static <T> ApiResponse<T> ok(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, meta, List.of(), null);
    }

    public static <T> ApiResponse<T> ok(T data, PageMeta meta, List<String> warnings) {
        return new ApiResponse<>(true, data, meta, warnings == null ? List.of() : List.copyOf(warnings), null);
    }

    public static <T> ApiResponse<T> fail(ErrorBody error) {
        return new ApiResponse<>(false, null, null, List.of(), error);
    }
}
