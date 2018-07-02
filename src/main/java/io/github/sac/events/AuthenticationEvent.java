package io.github.sac.events;

import io.github.sac.Socket;

/**
 * Created by sachin on 3/7/18.
 */
public interface AuthenticationEvent {
    void onAuthenticated(Socket socket, String token);
    void onDeauthentication(Socket socket);
}
