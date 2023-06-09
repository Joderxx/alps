package org.alps.core;

import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.alps.core.frame.ForgetFrame;
import org.alps.core.frame.RequestFrame;
import org.alps.core.socket.netty.server.NettyAlpsServer;
import org.alps.core.socket.netty.server.NettyServerConfig;

import java.util.Map;

@Slf4j
public class Server {

    public static void main(String[] args) {
        var nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setPort(6195);
        nettyServerConfig.setOptionSettings(Map.of(
                ChannelOption.SO_BACKLOG, 128
        ));
        nettyServerConfig.setChildOptionSettings(Map.of(
                ChannelOption.SO_KEEPALIVE, true
        ));
        var routerDispatcher = new RouterDispatcher();
        var config = new AlpsConfig();
        for (int i = 0; i < 10; i++) {
            short module = (short) i;
            config.getModules().add(new AlpsConfig.ModuleConfig((short) i, (short) 1, 1L));
            for (int j = 0; j < 20; j++) {
                short command = (short) j;
                routerDispatcher.addRouter(new Router() {
                    @Override
                    public short module() {
                        return module;
                    }

                    @Override
                    public int command() {
                        return command;
                    }

                    @Override
                    public void handle(AlpsEnhancedSession session, CommandFrame frame) {
//                        log.info("Command Received: " + command);
//                        try {
//                            TimeUnit.MICROSECONDS.sleep(20L);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
                        if (frame instanceof ForgetFrame forgetFrame) {

                        } else if (frame instanceof RequestFrame requestFrame) {
                            session.response()
                                    .reqId(frame.id())
                                    .data("Hello")
                                    .send();
                        }
                    }
                });
            }
        }
        var enhancedSessionFactory = new DefaultEnhancedSessionFactory(routerDispatcher, config);
        var server = new NettyAlpsServer(new NioEventLoopGroup(1),
                new NioEventLoopGroup(32),
                new NioEventLoopGroup(32),
                nettyServerConfig, enhancedSessionFactory,
                enhancedSessionFactory.config.getModules().stream().map(AlpsConfig.ModuleConfig::getModule).toList());
        server.start();
    }

    static class DefaultEnhancedSessionFactory implements EnhancedSessionFactory {

        final FrameCoders frameCoders;
        final AlpsDataCoderFactory dataCoderFactory;
        final FrameListeners frameListeners;
        final AlpsConfig config;

        DefaultEnhancedSessionFactory(RouterDispatcher routerDispatcher, AlpsConfig config) {
            this.dataCoderFactory = new AlpsDataCoderFactory();
            this.frameListeners = new FrameListeners(routerDispatcher);
            this.config = config;
            this.frameCoders = new FrameCoders(dataCoderFactory);
        }

        @Override
        public AlpsEnhancedSession create(AlpsSession session) {
            return new AlpsEnhancedSession(session, frameCoders, dataCoderFactory, frameListeners, config);
        }
    }
}
