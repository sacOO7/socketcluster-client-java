package io.github.sac.events;

import com.neovisionaries.ws.client.WebSocketFrame;
import io.github.sac.Socket;

/**
 * Created by sachin on 3/7/18.
 */

public interface DisconnectEvent {
    void onDisconnected(Socket socket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer);
}
