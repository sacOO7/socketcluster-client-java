package io.github.sac;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.util.List;
import java.util.Map;

/**
 * Created by sachin on 13/11/16.
 */
public interface BasicListener{
    public void onConnected(Map<String, List<String>> headers);
    public void onDisconnected(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer);
    public void onConnectError(WebSocketException exception);
    public void onAuthentication(Boolean status);
    public void onSetAuthToken(String token, Socket socket);
}
