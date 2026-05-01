package com.petshop.order.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.time.LocalDateTime;
import java.util.Properties;

@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AutoFillInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object entity = invocation.getArgs()[1];

        if (entity != null) {
            LocalDateTime now = LocalDateTime.now();
            SqlCommandType cmdType = ms.getSqlCommandType();

            if (cmdType == SqlCommandType.INSERT) {
                setField(entity, "createTime", now);
                setField(entity, "updateTime", now);
            } else if (cmdType == SqlCommandType.UPDATE) {
                setField(entity, "updateTime", now);
            }
        }

        return invocation.proceed();
    }

    private void setField(Object entity, String fieldName, Object value) {
        try {
            var field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.get(entity) == null || "updateTime".equals(fieldName)) {
                field.set(entity, value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在则跳过
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
