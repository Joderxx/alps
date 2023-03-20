package org.alps.core.listener;

import lombok.extern.slf4j.Slf4j;
import org.alps.core.*;

@Slf4j
public class RequestFrameListener implements FrameListener {

    private final RouterDispatcher routerDispatcher;


    public RequestFrameListener(RouterDispatcher routerDispatcher) {
        this.routerDispatcher = routerDispatcher;
    }

    @Override
    public void listen(AlpsSession session, Frame frame) {
        routerDispatcher.dispatch(((AlpsEnhancedSession) session), ((CommandFrame) frame));
    }

}
