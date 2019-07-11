package com.loveincode.chat.util;

import java.net.SocketAddress;

import io.netty.channel.Channel;

/**
 * @author huyifan
 */
public class NettyUtil {

    /**
     * 获取Channel的远程IP地址
     *
     * @param channel channel
     * @return 远程IP地址
     */
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }
}
