package io.github.sac.events;

import com.neovisionaries.ws.client.WebSocketException;
import io.github.sac.Socket;

/**
 * Created by sachin on 3/7/18.
 */
public interface ConnectionAbort {
    void onConnectionAbort(Socket socket, WebSocketException exception);
}
