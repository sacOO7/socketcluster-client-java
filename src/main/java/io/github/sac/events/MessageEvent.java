package io.github.sac.events;

/**
 * Created by sachin on 3/7/18.
 */
public interface MessageEvent {
    void onMessage(String name, Object error, Object data);
}
