package com.xuesinuo.muppet.config.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class ServiceException extends RuntimeException {
    private final String message;
}
