package com.example.dynamicdatasource.custom.config;

import com.example.dynamicdatasource.custom.datasource.DynamicDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义多数据源配置
 *
 * 仅在 "custom" profile 激活时生效。
 * 手动创建 master 和 slave 两个物理数据源，
 * 然后注册为一个支持动态切换的 DynamicDataSource。
 */
@Configuration
@Profile("custom")
public class CustomDataSourceConfig {

    /** master 数据源 —— 负责写操作 */
    @Bean
    @ConfigurationProperties(prefix = "custom.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    /** slave 数据源 —— 负责读操作 */
    @Bean
    @ConfigurationProperties(prefix = "custom.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 动态数据源 —— @Primary 让 MyBatis Plus 使用它
     *
     * 内部维护一个 Map<lookupKey, DataSource>，
     * 每次获取连接时通过 determineCurrentLookupKey() 决定路由到哪个物理数据源。
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource());
        targetDataSources.put("slave", slaveDataSource());

        dynamicDataSource.setTargetDataSources(targetDataSources);
        // 默认走主库
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource());

        return dynamicDataSource;
    }
}
