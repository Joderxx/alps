package org.alps.core;

import org.alps.core.frame.ForgetFrame;
import org.alps.core.frame.RequestFrame;
import org.alps.core.frame.ResponseFrame;
import org.openjdk.jmh.annotations.*;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 10)
@Fork(1)
public class AlpsEnhancedSessionBenchmark {

    @Benchmark
    public void forget(SessionState state) throws ExecutionException, InterruptedException {
        state.session.forget(1)
                .data("1234".repeat(1024))
                .send().get();
    }

    @Benchmark
    public void request(SessionState state) throws ExecutionException, InterruptedException {
        state.session.forget(1)
                .data("1234".repeat(1024))
                .send().get();
    }


    @State(Scope.Benchmark)
    public static class SessionState {
        AlpsEnhancedSession session;

        @Setup
        public void setup() {
            var session = new Session();
            var dataCoderFactory = new AlpsDataCoderFactory();
            var frameFactory = new FrameCoders(dataCoderFactory);
            var routerDispatcher = new RouterDispatcher();
            var listenerHandler = new FrameListeners(routerDispatcher);
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
                            if (frame instanceof ForgetFrame forgetFrame) {

                            } else if (frame instanceof RequestFrame requestFrame) {
                                var metadata = requestFrame.metadata();
                                int id = session.nextId();
                                session.receive(new ResponseFrame(id, requestFrame.id(),
                                        new AlpsMetadataBuilder().isZip(metadata.isZip())
                                                .verifyToken(metadata.verifyToken())
                                                .version(metadata.version())
                                                .containerCoder(metadata.containerCoder())
                                                .coder(metadata.coder())
                                                .frameType(FrameCoders.DefaultFrame.RESPONSE.frameType)
                                                .frame(ResponseFrame.toBytes(id, requestFrame.id()))
                                                .build()
                                        , requestFrame.data()));
                            }
                        }
                    });
                }
            }

            this.session = new AlpsEnhancedSession(session, frameFactory, dataCoderFactory, listenerHandler, config);
        }

    }

    static final class Session implements AlpsSession {

        @Override
        public short module() {
            return 0;
        }

        @Override
        public InetAddress selfAddress() {
            return null;
        }

        @Override
        public InetAddress targetAddress() {
            return null;
        }

        @Override
        public void send(AlpsPacket protocol) {

        }

        @Override
        public void close() {

        }

        @Override
        public <T> T attr(String key) {
            return null;
        }

        @Override
        public AlpsSession attr(String key, Object value) {
            return this;
        }
    }
}
