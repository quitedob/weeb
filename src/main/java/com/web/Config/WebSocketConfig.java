package com.web.Config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 配置类
 * 基于Netty实现WebSocket服务器
 */
@Slf4j
@Component
public class WebSocketConfig implements ApplicationListener<ContextClosedEvent> {

    @Value("${websocket.port:8081}")
    private int websocketPort;

    @Value("${websocket.path:/ws}")
    private String websocketPath;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannelFuture;

    @PostConstruct
    public void start() {
        new Thread(this::startServer).start();
    }

    private void startServer() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 添加处理器
                            ch.pipeline()
                                    // HTTP编解码器
                                    .addLast(new HttpServerCodec())
                                    // 对HTTP消息进行聚合，形成FullHttpRequest或FullHttpResponse
                                    .addLast(new HttpObjectAggregator(65536))
                                    // 支持大文件传输
                                    .addLast(new ChunkedWriteHandler())
                                    // 心跳检测
                                    .addLast(new IdleStateHandler(60, 60, 90, TimeUnit.SECONDS))
                                    // WebSocket协议处理器
                                    .addLast(new WebSocketServerProtocolHandler(websocketPath, null, true, 65536))
                                    // 自定义WebSocket处理器
                                    .addLast(webSocketHandler);
                        }
                    });

            // 启动服务器
            serverChannelFuture = bootstrap.bind(websocketPort).sync();
            log.info("WebSocket服务器启动成功，端口：{}，路径：{}", websocketPort, websocketPath);

            // 等待服务器socket关闭
            serverChannelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            log.error("WebSocket服务器启动被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("WebSocket服务器启动失败", e);
        } finally {
            shutdown();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (serverChannelFuture != null) {
            serverChannelFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        log.info("WebSocket服务器已关闭");
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        shutdown();
    }
}