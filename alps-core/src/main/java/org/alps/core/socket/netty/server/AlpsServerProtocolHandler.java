package org.alps.core.socket.netty.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.alps.core.AlpsPacket;
import org.alps.core.EnhancedSessionFactory;
import org.alps.core.socket.netty.AlpsNioSocketChannel;
import org.alps.core.socket.netty.NettyAlpsSession;
import org.alps.core.socket.netty.RemotingHelper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class AlpsServerProtocolHandler extends SimpleChannelInboundHandler<AlpsPacket> {

    private final NettyAlpsServer server;
    private final EnhancedSessionFactory sessionFactory;
    private final List<Short> supportModules;

    public AlpsServerProtocolHandler(NettyAlpsServer server, EnhancedSessionFactory sessionFactory, List<Short> supportModules) {
        this.server = server;
        this.sessionFactory = sessionFactory;
        this.supportModules = supportModules;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("register channel, info: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("unregister channel, info: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("active channel, info: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("Register an inactive channel");
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.debug("add client! client info: {}", ctx.channel().remoteAddress());
        // 初始化所有连接对象
        var map = ctx.channel().attr(RemotingHelper.KEY).get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ctx.channel().attr(RemotingHelper.KEY).set(map);
        }
        map.computeIfAbsent(AlpsPacket.ZERO_MODULE,
                key -> {
                    var sess = sessionFactory.create(new NettyAlpsSession(server, AlpsPacket.ZERO_MODULE, ((AlpsNioSocketChannel) ctx.channel())));
                    this.server.addSession(sess);
                    return sess;
                });
        for (Short module : supportModules) {
            map.computeIfAbsent(module,
                    key -> {
                        var sess = sessionFactory.create(new NettyAlpsSession(server, module, ((AlpsNioSocketChannel) ctx.channel())));
                        this.server.addSession(sess);
                        return sess;
                    });
        }

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.debug("remove client! client info: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("thrown an error! ", cause);
        RemotingHelper.closeChannel(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AlpsPacket msg) throws Exception {
        var map = ctx.channel().attr(RemotingHelper.KEY).get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ctx.channel().attr(RemotingHelper.KEY).set(map);
        }
        var session = map.computeIfAbsent(msg.module(),
                key -> {
                    var sess = sessionFactory.create(new NettyAlpsSession(server, msg.module(), ((AlpsNioSocketChannel) ctx.channel())));
                    this.server.addSession(sess);
                    return sess;
                });
        session.receive(msg);
    }
}
