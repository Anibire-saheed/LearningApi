package com.mosque.payment.data.dto.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse implements Serializable {

    public int statusCode;
    public String description;
    public Object data;
    public Object errors;
}
