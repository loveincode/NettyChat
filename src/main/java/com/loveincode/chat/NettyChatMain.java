package com.loveincode.chat;

import com.loveincode.chat.util.Constants;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket聊天室，客户端参考docs目录下的websocket.html
 *
 * @author huyifan
 */
@Slf4j
public class NettyChatMain {

    public static void main(String[] args) {
        final NettyChatServer server = new NettyChatServer(Constants.DEFAULT_PORT);
        server.init();
        server.start();
        // 注册进程钩子，在JVM进程关闭前释放资源
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown();
                log.warn(">>>>>>>>>> jvm shutdown");
                System.exit(0);
            }
        });
    }
}
