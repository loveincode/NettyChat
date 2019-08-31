package com.loveincode.chat.handler;

import com.alibaba.fastjson.JSONObject;
import com.loveincode.chat.entity.UserInfo;
import com.loveincode.chat.proto.ChatCode;
import com.loveincode.chat.proto.Constants;
import com.loveincode.chat.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理握手和认证
 *
 * @author huyifan
 */
@Slf4j
public class UserAuthHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handShaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            //处理http请求
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            //处理socket请求
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * ChannelInboundHandlerAdapter  userEventTriggered
     * @param ctx
     * @param evt
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (event.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                UserInfoManager.removeChannel(ctx.channel());
                UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("handleHttpRequest 收到消息:{}", request);
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            log.warn("protobuf don't support websocket");
            ctx.channel().close();
            return;
        }
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
            Constants.WEBSOCKET_URL, null, true);
        handShaker = handshakerFactory.newHandshaker(request);
        if (handShaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 动态加入websocket的编解码处理
            handShaker.handshake(ctx.channel(), request);
            UserInfo userInfo = new UserInfo();
            userInfo.setAddr(NettyUtil.parseChannelRemoteAddr(ctx.channel()));
            // 存储已经连接的Channel
            log.info("**存储已经连接的Channel");
            UserInfoManager.addChannel(ctx.channel());
        }
    }

    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        log.info("handleWebSocket 收到消息:{}", frame.content().retain());
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame) {
            handShaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            log.info("close websocket removeChannel :{}", ctx.channel());
            UserInfoManager.removeChannel(ctx.channel());
            return;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame) {
            log.info("ping message:{}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            log.info("pong message:{}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 本程序目前只支持文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }
        String message = ((TextWebSocketFrame) frame).text();
        UserInfo userInfo = UserInfoManager.getUserInfo(ctx.channel());
        log.info("**收到用户 " + userInfo.toString() + "消息**" + message);
        JSONObject json = JSONObject.parseObject(message);
        String code = json.getString("code");

        Channel channel = ctx.channel();
        switch (code) {
            case ChatCode.PING_CODE:
            case ChatCode.PONG_CODE:
                UserInfoManager.updateUserTime(channel);
                //UserInfoManager.sendPong(ctx.channel());
                log.info("receive pong message, address: {}", NettyUtil.parseChannelRemoteAddr(channel));
                return;
            case ChatCode.AUTH_CODE:
                boolean isSuccess = UserInfoManager.saveUser(channel, json.getString("nick"));
                UserInfoManager.sendInfo(channel, ChatCode.SYS_AUTH_STATE, isSuccess);
                if (isSuccess) {
                    UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
                }
                return;
            //普通的消息留给MessageHandler处理
            case ChatCode.MESS_CODE:
                break;
            default:
                log.warn("The code [{}] can't be auth!!!", code);
                return;
        }
        //后续消息交给MessageHandler处理
        ctx.fireChannelRead(frame.retain());
    }
}
