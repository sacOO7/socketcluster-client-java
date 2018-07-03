package io.github.sac;

/**
 * Created by sachin on 13/11/16.
 */

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Emitter {

    private boolean multipleListenersEnabled;
    private boolean multipleChannelWatchersEnabled;

    public Emitter(boolean multipleListenersEnabled, boolean multipleChannelWatchersEnabled) {
        this.multipleListenersEnabled = multipleListenersEnabled;
        this.multipleChannelWatchersEnabled = multipleChannelWatchersEnabled;
    }

    public void setMultipleListenersEnabled(boolean multipleListenersEnabled) {
        this.multipleListenersEnabled = multipleListenersEnabled;
    }

    public boolean isMultipleListenersEnabled() {
        return multipleListenersEnabled;
    }

    public boolean isMultipleChannelWatchersEnabled() {
        return multipleChannelWatchersEnabled;
    }

    public void setMultipleChannelWatchersEnabled(boolean multipleChannelWatchersEnabled) {
        this.multipleChannelWatchersEnabled = multipleChannelWatchersEnabled;
    }

    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Listener>> listeners = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<AckListener>> ackListeners = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Listener>> channelObservers = new ConcurrentHashMap<>();

    /**
     * Listens on the event.
     *
     * @param event event name.
     * @return a reference to this object.
     */
    public Emitter on(String event, Listener fn) {
        return on(event, fn, multipleListenersEnabled);
    }

    public Emitter on(String event, Listener fn, boolean multiListenersEnabled) {
        registerEvent(listeners, event, fn, multiListenersEnabled);
        return this;
    }

    public Emitter on(String event, AckListener fn) {
        return on(event, fn, multipleListenersEnabled);
    }

    public Emitter on(String event, AckListener fn, boolean multiListenersEnabled) {
        registerEvent(ackListeners, event, fn, multiListenersEnabled);
        return this;
    }

    public Emitter onSubscribe(String event, Listener fn) {
        return onSubscribe(event, fn, multipleChannelWatchersEnabled);
    }

    public Emitter onSubscribe(String event, Listener fn, boolean multipleChannelWatchersEnabled) {
        registerEvent(channelObservers, event, fn, multipleChannelWatchersEnabled);
        return this;
    }


    private static <T> void registerEvent(ConcurrentHashMap<String, ConcurrentLinkedQueue<T>> listeners, String event, T fn, boolean multiEnabled) {
        if (listeners.containsKey(event)) {
            if (!multiEnabled) {
                listeners.get(event).clear();
            }
            listeners.get(event).add(fn);
            return;
        }
        ConcurrentLinkedQueue<T> linkedListeners = new ConcurrentLinkedQueue<>();
        linkedListeners.add(fn);
        listeners.put(event, linkedListeners);
    }


    public Emitter handleEmit(String event, Object object) {
        handleEvent(listeners, event, object, null);
        return this;
    }


    public Emitter handleEmitAck(String event, Object object, Ack ack) {
        handleEvent(ackListeners, event, object, ack);
        return this;
    }

    public Emitter handlePublish(String event, Object object) {
        handleEvent(channelObservers, event, object, null);
        return this;
    }


    public static <T> void handleEvent(ConcurrentHashMap<String, ConcurrentLinkedQueue<T>> listeners, String event, Object object, Ack ack) {
        Iterator<T> listenerIterator = listeners.get(event).iterator();
        while (listenerIterator.hasNext()) {
            T listener = listenerIterator.next();
            if (listener instanceof Listener) {
                ((Listener) listener).call(event, object);
            } else {
                ((AckListener) listener).call(event, object, ack);
            }
        }
    }

    public void off(String event) {
        listeners.remove(event);
        ackListeners.remove(event);
    }

    public void off(String event, Listener listener) {
        if (listeners.containsKey(event)) {
            listeners.get(event).remove(listener);
        }
    }

    public void off(String event, AckListener ackListener) {
        if (ackListeners.containsKey(event)) {
            ackListeners.get(event).remove(ackListener);
        }
    }

    public void removeAllListeners() {
        for (Map.Entry e : listeners.entrySet()) {
            listeners.remove(e.getKey().toString());
        }
        for (Map.Entry e : ackListeners.entrySet()) {
            ackListeners.remove(e.getKey().toString());
        }
        for (Map.Entry e : channelObservers.entrySet()) {
            channelObservers.remove(e.getKey().toString());
        }
    }


    public interface Listener {
        void call(String name, Object data);
    }

    public interface AckListener {
        void call(String name, Object data, Ack ack);
    }

}

