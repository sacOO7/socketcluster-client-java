package io.github.sac.events;

/**
 * Created by sachin on 3/7/18.
 */

public interface RawEvent {
    void onRawEvent(Object error, Object data);
}
