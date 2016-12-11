package com.jakduk.api.restcontroller.exception;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.jakduk.core.exception.ServiceExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * @author pyohwan
 * 16. 3. 5 오전 12:31
 */

@AllArgsConstructor
@Getter
public class ApiRestErrorResponse {

    private String code;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, String> fields;

    public ApiRestErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiRestErrorResponse(ServiceExceptionCode serviceExceptionCode) {
        this.code = serviceExceptionCode.getCode();
        this.message = serviceExceptionCode.getMessage();
    }

    public ApiRestErrorResponse(ServiceExceptionCode serviceExceptionCode, Map<String, String> fields) {
        this.code = serviceExceptionCode.getCode();
        this.message = serviceExceptionCode.getMessage();
        this.fields = fields;
    }
}