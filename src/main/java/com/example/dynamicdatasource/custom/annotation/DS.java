package com.example.dynamicdatasource.custom.annotation;

import java.lang.annotation.*;

/**
 * 自定义数据源切换注解
 * 用在 Service 方法上，声明该方法要使用的数据源名称
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /** 数据源名称，如 "master"、"slave" */
    String value();
}
