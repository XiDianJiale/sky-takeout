# 基础镜像使用 Java 11 (Alpine 版本体积更小)
FROM eclipse-temurin:11-jre-alpine

# 设置容器内的工作目录
WORKDIR /app

# 将本地 target 目录下的 jar 包复制到容器的 /app 目录下，并重命名为 app.jar
# 注意：前提是你已经在本地执行了 mvn package
COPY sky-server/target/sky-server-1.0-SNAPSHOT.jar app.jar

# 声明暴露的端口
EXPOSE 8080

# 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]