package com.myy.weitutravel.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CodeEnum {

    SUCCESS(200, "成功"),
    FAIL(400, "失败");

    final int code;
    final String msg;
}
