package com.dts.scheduler.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpPayloadDto {
    private String url;
    @Builder.Default
    private String method = "GET";
    private Map<String, String> headers;
    private String body;
}
