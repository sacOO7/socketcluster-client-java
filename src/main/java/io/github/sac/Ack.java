package io.github.sac; /**
 * Created by sachin on 16/11/16.
 */

/**
 *  Interface for handling errors
 *
 */

public interface Ack {
    void call(String name, Object error, Object data);
}
