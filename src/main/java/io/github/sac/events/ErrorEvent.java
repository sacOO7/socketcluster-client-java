package io.github.sac.events;

import io.github.sac.Socket;

/**
 * Created by sachin on 3/7/18.
 */

public interface ErrorEvent {
    void onError(Socket socket, Exception error);
}
