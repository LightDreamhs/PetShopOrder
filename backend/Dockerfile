# syntax=docker/dockerfile:1
# 后端镜像：多阶段构建（Maven 编译 → 精简 JRE 运行）

# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先拷 pom，利用层缓存预下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
# 拷源码并打包（pom 中 finalName=app → target/app.jar）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre-alpine
# 时区设为上海，避免订单 create_time 回退 UTC（曾导致 Admin 新订单不弹通知）
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone
# 非 root 用户（uid 1000，与宿主机 /data/petshop 的 owner 对齐，保证 uploads 写入权限）
RUN addgroup -g 1000 -S app && adduser -u 1000 -S app -G app
WORKDIR /app
RUN mkdir -p /app/uploads && chown -R app:app /app
COPY --from=builder /build/target/app.jar /app/app.jar
USER app
# JVM 内存上限（单机与 mysql 共存，限制堆避免争用）+ 时区
ENV JAVA_OPTS="-Xmx512m -Duser.timezone=Asia/Shanghai"
EXPOSE 8080
# 容器健康探针：调用 /health 端点（HealthController）
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/health || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
