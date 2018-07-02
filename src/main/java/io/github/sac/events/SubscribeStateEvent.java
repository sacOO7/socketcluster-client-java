package io.github.sac.events;

import io.github.sac.Socket;

/**
 * Created by sachin on 3/7/18.
 */
public interface SubscribeStateEvent {
    void onSubscribeStateChange(Socket.Channel channel, Socket.ChannelState oldState, Socket.ChannelState newState);
}
