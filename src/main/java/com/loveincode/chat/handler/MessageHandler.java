package com.loveincode.chat.handler;

import com.alibaba.fastjson.JSONObject;
import com.loveincode.chat.entity.UserInfo;
import com.loveincode.chat.proto.ChatCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理消息的发送
 *
 * @author huyifan
 */
@Slf4j
public class MessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        UserInfo userInfo = UserInfoManager.getUserInfo(ctx.channel());
        if (userInfo != null && userInfo.isAuth()) {
            JSONObject json = JSONObject.parseObject(frame.text());
            // 广播返回用户发送的消息文本
            UserInfoManager.broadcastMess(userInfo.getUserId(), userInfo.getNick(), json.getString("mess"));
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
        super.channelUnregistered(ctx);
    }

    /**
     * 异常发生时
     * @param ctx
     * @param cause
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("connection error and close the channel", cause);
        UserInfoManager.removeChannel(ctx.channel());
        UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
    }

}
