package io.github.sac;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.util.List;
import java.util.Map;

/**
 * Created by sachin on 13/11/16.
 */
public interface BasicListener{
    void onConnected(Socket socket, Map<String, List<String>> headers);
    void onDisconnected(Socket socket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer);
    void onConnectError(Socket socket, WebSocketException exception);
    void onAuthentication(Socket socket, Boolean status);
    void onSetAuthToken(String token, Socket socket);
}
