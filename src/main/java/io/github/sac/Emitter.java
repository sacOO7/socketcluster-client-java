package io.github.sac;

/**
 * Created by sachin on 13/11/16.
 */

import java.util.concurrent.ConcurrentHashMap;

public class Emitter {


    private ConcurrentHashMap<String,Listener> singlecallbacks=new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,AckListener> singleackcallbacks=new ConcurrentHashMap<>();
    private ConcurrentHashMap<String,Listener> publishcallbacks=new ConcurrentHashMap<>();

    /**
     * Listens on the event.
     * @param event event name.
     * @param fn
     * @return a reference to this object.
     */
    public Emitter on(String event, Listener fn) {

        if (singlecallbacks.get(event)!=null){
            singlecallbacks.remove(event);
        }
            singlecallbacks.put(event, fn);
        return this;
    }

    public Emitter onSubscribe(String event,Listener fn){

        if (publishcallbacks.get(event)!=null){
            publishcallbacks.remove(event);
        }
        publishcallbacks.put(event, fn);
        return this;
    }

    public Emitter on(String event, AckListener fn) {
        if (singleackcallbacks.get(event)!=null){
            singleackcallbacks.remove(event);
        }
        singleackcallbacks.put(event, fn);
        return this;
    }



    public Emitter handleEmit(String event, Object object) {

        Listener listener=singlecallbacks.get(event);
        if (listener!=null){
            listener.call(event,object);
        }
        return this;
    }

    public Emitter handlePublish(String event, Object object){

        Listener listener=publishcallbacks.get(event);

        if (listener!=null){
            listener.call(event,object);
        }
        return this;
    }

    public boolean hasEventAck(String event){
        return this.singleackcallbacks.get(event)!=null;
    }

    public Emitter handleEmitAck(String event, Object object , Ack ack){

        AckListener listener=singleackcallbacks.get(event);
        if (listener!=null){
            listener.call(event,object,ack);
        }
        return this;
    }



    public interface Listener {
        void call(String name,Object data);
    }

    public interface AckListener {
        void call (String name,Object data,Ack ack);
    }

}

