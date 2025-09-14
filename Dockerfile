# 使用官方的OpenJDK 17镜像作为基础镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置时区为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制Maven包装器和POM文件
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# 下载依赖（利用Docker缓存层）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests

# 查找构建的JAR文件并重命名
RUN find target -name "*.jar" -not -name "*-sources.jar" -exec cp {} app.jar \;

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]

