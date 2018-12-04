package io.github.sac;

import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by sachin on 13/11/16.
 */
public interface BasicListener {

    void onConnected(Socket socket, Headers headers);

    void onDisconnected(Socket socket, int code, String reason);

    void onConnectError(Socket socket, Throwable throwable, Response response);

    void onAuthentication(Socket socket, Boolean status);

    void onSetAuthToken(String token, Socket socket);
}
