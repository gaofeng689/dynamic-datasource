package com.example.dynamicdatasource.custom.datasource;

import com.example.dynamicdatasource.custom.context.DynamicDataSourceContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源 —— 继承 AbstractRoutingDataSource
 *
 * 核心原理：
 * 每次获取数据库连接时，Spring 都会调用 determineCurrentLookupKey()，
 * 该方法返回的 key 决定了实际使用哪个数据源。
 *
 * 本实现从 DynamicDataSourceContextHolder（ThreadLocal）中获取 key，
 * 从而实现不同线程可以使用不同数据源。
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.get();
    }
}
