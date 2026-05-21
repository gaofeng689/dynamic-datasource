package com.example.dynamicdatasource.custom.context;

/**
 * 数据源上下文持有者 —— 基于 ThreadLocal 实现线程级数据源隔离
 *
 * 每个请求线程独立持有自己的数据源标识，互不干扰。
 * 这是动态数据源实现的核心：所有数据源切换都通过这个 ThreadLocal 变量来控制。
 */
public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    /** 设置当前线程使用的数据源 */
    public static void set(String dataSourceName) {
        CONTEXT.set(dataSourceName);
    }

    /** 获取当前线程的数据源标识 */
    public static String get() {
        return CONTEXT.get();
    }

    /** 清除数据源标识（防止内存泄漏和线程复用时串号） */
    public static void clear() {
        CONTEXT.remove();
    }
}
