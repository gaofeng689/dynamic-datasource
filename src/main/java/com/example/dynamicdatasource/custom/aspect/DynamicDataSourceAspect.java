package com.example.dynamicdatasource.custom.aspect;

import com.example.dynamicdatasource.custom.annotation.DS;
import com.example.dynamicdatasource.custom.context.DynamicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据源切换 AOP 切面
 *
 * 拦截所有标注了 @DS 注解的方法。
 * 在方法执行前设置数据源，方法执行后清除。
 *
 * @Order(-1) 确保在 @Transactional 之前执行，否则事务已经绑定连接，
 * 切换数据源就无效了。
 */
@Aspect
@Component
@Order(-1)
public class DynamicDataSourceAspect {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceAspect.class);

    /** 切点：匹配标注了 @DS 注解的方法 */
    @Pointcut("@annotation(com.example.dynamicdatasource.custom.annotation.DS)")
    public void dsPointcut() {
    }

    @Around("dsPointcut() && @annotation(ds)")
    public Object around(ProceedingJoinPoint pjp, DS ds) throws Throwable {
        String dataSourceName = ds.value();
        log.debug("切换数据源 → {}", dataSourceName);

        DynamicDataSourceContextHolder.set(dataSourceName);
        try {
            return pjp.proceed();
        } finally {
            DynamicDataSourceContextHolder.clear();
            log.debug("清除数据源");
        }
    }
}
