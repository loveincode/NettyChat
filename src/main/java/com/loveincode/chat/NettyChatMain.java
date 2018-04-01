package com.loveincode.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.loveincode.chat.util.Constants;

/**
 * WebSocket聊天室，客户端参考docs目录下的websocket.html
 */
public class NettyChatMain {
    private static final Logger logger = LoggerFactory.getLogger(NettyChatMain.class);

    public static void main(String[] args) {
        final NettyChatServer server = new NettyChatServer(Constants.DEFAULT_PORT);
        server.init();
        server.start();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                server.shutdown();
                logger.warn(">>>>>>>>>> jvm shutdown");
                System.exit(0);
            }
        });
    }
}
