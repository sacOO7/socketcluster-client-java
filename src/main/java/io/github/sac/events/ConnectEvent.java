package io.github.sac.events;

import io.github.sac.Socket;
import java.util.List;
import java.util.Map;

/**
 * Created by sachin on 3/7/18.
 */
public interface ConnectEvent {
    void onConnected(Socket socket, Map<String, List<String>> headers);
}
