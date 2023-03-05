package io.github.sac;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.StatusLine;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sachin on 13/11/16.
 */

public class Socket extends Emitter {

    private final Logger logger = Logger.getLogger(Socket.class.getName());

    private AtomicInteger counter;
    private String URL;
    private WebSocketFactory factory;
    private ReconnectStrategy strategy;
    private WebSocket ws;
    private BasicListener listener;
    private String AuthToken;
    private HashMap<Long, Object[]> acks;
    private ConcurrentHashMap<String, Channel> channels;
    private WebSocketAdapter adapter;
    private Map<String, String> headers;

    public Socket(String URL) {
        this.URL = URL;
        factory = new WebSocketFactory().setConnectionTimeout(5000);
        counter = new AtomicInteger(1);
        acks = new HashMap<>();
        channels = new ConcurrentHashMap<>();
        adapter = getAdapter();
        headers = new HashMap<>();
        putDefaultHeaders();
    }

    private void putDefaultHeaders() {
        headers.put("Accept-Encoding", "gzip, deflate, sdch");
        headers.put("Accept-Language", "en-US,en;q=0.8");
        headers.put("Pragma", "no-cache");
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");
    }

    public Channel createChannel(String name) {
        if(channels.containsKey(name)){
            return channels.get(name);
        }

        Channel channel = new Channel(name);
        channels.put(name, channel);
        return channel;
    }

    public ConcurrentHashMap<String, Channel> getChannels() {
        return channels;
    }

    public Channel getChannelByName(String name) {
        return channels.get(name);
    }

    public void seturl(String url) {
        this.URL = url;
    }

    public void setReconnection(ReconnectStrategy strategy) {
        this.strategy = strategy;
    }

    public void setListener(BasicListener listener) {
        this.listener = listener;
    }

    public Logger getLogger(){
        return logger;
    }
    /**
     * used to set up TLS/SSL connection to server for more details visit neovisionaries websocket client
     */

    public WebSocketFactory getFactorySettings() {
        return factory;
    }

    public void setAuthToken(String token) {
        AuthToken = token;
    }

    public WebSocketAdapter getAdapter() {
        return new WebSocketAdapter() {

            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {

                /**
                 * Code for sending handshake
                 */

                counter.set(1);
                if (strategy != null) {
                    strategy.setAttemptsMade(0);
                }

                JSONObject handshakeObject = new JSONObject();
                handshakeObject.put("event", "#handshake");
                JSONObject object = new JSONObject();
                object.put("authToken", AuthToken);
                handshakeObject.put("data", object);
                handshakeObject.put("cid", counter.getAndIncrement());
                websocket.sendText(handshakeObject.toString());

                listener.onConnected(Socket.this, headers);

                super.onConnected(websocket, headers);
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                listener.onDisconnected(Socket.this, serverCloseFrame, clientCloseFrame, closedByServer);
                reconnect();
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                listener.onConnectError(Socket.this, exception);
                reconnect();
                super.onConnectError(websocket, exception);
            }

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                if (text == null) {
                    websocket.sendText("");
                }
                super.onTextMessage(websocket, text);
            }

            @Override
            public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

                    JSONObject object = new JSONObject(frame.getPayloadText());
                    /**
                     * Message retrieval mechanism goes here
                     */
                    logger.info("Message :" + object.toString());

                    try {
                        Object dataobject = object.opt("data");
                        Integer rid = (Integer) object.opt("rid");
                        Integer cid = (Integer) object.opt("cid");
                        String event = (String) object.opt("event");

                        switch (Parser.parse(dataobject, event)) {

                            case ISAUTHENTICATED:
                                listener.onAuthentication(Socket.this, ((JSONObject) dataobject).getBoolean("isAuthenticated"));
                                subscribeChannels();
                                break;
                            case PUBLISH:
                                Socket.this.handlePublish(((JSONObject) dataobject).getString("channel"), ((JSONObject) dataobject).opt("data"));
                                break;
                            case REMOVETOKEN:
                                setAuthToken(null);
                                break;
                            case SETTOKEN:
                                String token = ((JSONObject) dataobject).getString("token");
                                setAuthToken(token);
                                listener.onSetAuthToken(token, Socket.this);
                                break;
                            case EVENT:
                                if (hasEventAck(event)) {
                                    handleEmitAck(event, dataobject, ack(Long.valueOf(cid)));
                                } else {
                                    Socket.this.handleEmit(event, dataobject);

                                }
                                break;
                            case ACKRECEIVE:
                                if (acks.containsKey((long) rid)) {
                                    Object[] objects = acks.remove((long) rid);
                                    if (objects != null) {
                                        Ack fn = (Ack) objects[1];
                                        if (fn != null) {
                                            fn.call((String) objects[0], object.opt("error"), object.opt("data"));
                                        } else {
                                            logger.warning("ack function is null with rid " + rid);
                                        }
                                    }
                                }
                                break;
                        }
                    } catch (Exception e) {
                        logger.severe(e.toString());
                    }
                super.onFrame(websocket, frame);
            }


            @Override
            public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                logger.warning("On close frame got called");
                super.onCloseFrame(websocket, frame);
            }

            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                logger.severe("Error while sending data " + cause.toString());
                super.onSendError(websocket, cause, frame);
            }

        };

    }

    public Socket emit(final String event, final Object object) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject eventObject = new JSONObject();
                try {
                    eventObject.put("event", event);
                    eventObject.put("data", object);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(eventObject.toString());
            }
        });
        return this;
    }


    public Socket emit(final String event, final Object object, final Ack ack) {

        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject eventObject = new JSONObject();
                acks.put(counter.longValue(), getAckObject(event, ack));
                try {
                    eventObject.put("event", event);
                    eventObject.put("data", object);
                    eventObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(eventObject.toString());
            }
        });
        return this;
    }

    private Socket subscribe(final String channel) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject = new JSONObject();
                try {
                    subscribeObject.put("event", "#subscribe");
                    JSONObject object = new JSONObject();
                    object.put("channel", channel);
                    subscribeObject.put("data", object);

                    subscribeObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(subscribeObject.toString());
            }
        });
        return this;
    }

    private Object[] getAckObject(String event, Ack ack) {
        Object object[] = {event, ack};
        return object;
    }

    private Socket subscribe(final String channel, final Ack ack) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject = new JSONObject();
                try {
                    subscribeObject.put("event", "#subscribe");
                    JSONObject object = new JSONObject();
                    acks.put(counter.longValue(), getAckObject(channel, ack));
                    object.put("channel", channel);
                    subscribeObject.put("data", object);
                    subscribeObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(subscribeObject.toString());
            }
        });
        return this;
    }

    private Socket unsubscribe(final String channel) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject = new JSONObject();
                try {
                    subscribeObject.put("event", "#unsubscribe");
                    subscribeObject.put("data", channel);
                    subscribeObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(subscribeObject.toString());
            }
        });
        return this;
    }

    private Socket unsubscribe(final String channel, final Ack ack) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject = new JSONObject();
                try {
                    subscribeObject.put("event", "#unsubscribe");
                    subscribeObject.put("data", channel);

                    acks.put(counter.longValue(), getAckObject(channel, ack));
                    subscribeObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(subscribeObject.toString());
            }
        });
        return this;
    }

    public Socket publish(final String channel, final Object data) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject publishObject = new JSONObject();
                try {
                    publishObject.put("event", "#publish");
                    JSONObject object = new JSONObject();
                    object.put("channel", channel);
                    object.put("data", data);
                    publishObject.put("data", object);
                    publishObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(publishObject.toString());
            }
        });

        return this;
    }

    public Socket publish(final String channel, final Object data, final Ack ack) {
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject publishObject = new JSONObject();
                try {
                    publishObject.put("event", "#publish");
                    JSONObject object = new JSONObject();
                    acks.put(counter.longValue(), getAckObject(channel, ack));
                    object.put("channel", channel);
                    object.put("data", data);
                    publishObject.put("data", object);
                    publishObject.put("cid", counter.getAndIncrement());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ws.sendText(publishObject.toString());
            }
        });

        return this;
    }

    private Ack ack(final Long cid) {
        return new Ack() {
            public void call(final String channel, final Object error, final Object data) {
                EventThread.exec(new Runnable() {
                    public void run() {
                        JSONObject object = new JSONObject();
                        try {
                            object.put("error", error);
                            object.put("data", data);
                            object.put("rid", cid);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        ws.sendText(object.toString());
                    }
                });
            }
        };
    }


    private void subscribeChannels() {
        for(Map.Entry<String, Channel> entry : channels.entrySet()) {
            entry.getValue().subscribe();
        }
    }

    public void setExtraHeaders(Map<String, String> extraHeaders, boolean overrideDefaultHeaders) {
        if (overrideDefaultHeaders) {
            headers.clear();
        }

        headers.putAll(extraHeaders);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void connect() {

        if(ws == null || ws.getState() == WebSocketState.CLOSED) {
            try {
                ws = factory.createSocket(URL);
            } catch (IOException e) {
                logger.severe(e.toString());
            }
            ws.addExtension("permessage-deflate; client_max_window_bits");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                ws.addHeader(entry.getKey(), entry.getValue());
            }

            ws.addListener(adapter);

            try {
                ws.connect();
            } catch (OpeningHandshakeException e) {
                // A violation against the WebSocket protocol was detected
                // during the opening handshake.

                logger.severe(e.toString());
                // Status line.
                StatusLine sl = e.getStatusLine();
                logger.info("=== Status Line ===");
                logger.info("HTTP Version  = \n" + sl.getHttpVersion());
                logger.info("Status Code   = \n" + sl.getStatusCode());
                logger.info("Reason Phrase = \n" + sl.getReasonPhrase());

                // HTTP headers.
                Map<String, List<String>> headers = e.getHeaders();
                logger.info("=== HTTP Headers ===");
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    // Header name.
                    String name = entry.getKey();

                    // Values of the header.
                    List<String> values = entry.getValue();

                    if (values == null || values.size() == 0) {
                        // Print the name only.
                        logger.info(name);
                        continue;
                    }

                    for (String value : values) {
                        // Print the name and the value.
                        logger.info(name + value + "\n");
                    }
                }
            } catch (WebSocketException e) {
                e.printStackTrace();
                listener.onConnectError(Socket.this, e);
                reconnect();
            }
        }else{
            logger.warning("Unable to connect: there is an active connection");
        }

    }

    public void connectAsync() {
        if(ws == null || ws.getState() == WebSocketState.CLOSED){
            try {
                ws = factory.createSocket(URL);
            } catch (IOException e) {
                logger.severe(e.toString());
            }

            ws.addExtension("permessage-deflate; client_max_window_bits");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                ws.addHeader(entry.getKey(), entry.getValue());
            }

            ws.addListener(adapter);
            ws.connectAsynchronously();
        }else{
            logger.warning("Unable to connect: there is an active connection");
        }
    }

    private void reconnect() {
        if (strategy == null) {
            logger.warning("Unable to reconnect: reconnection is null");
            return;
        }

        if (strategy.areAttemptsComplete()) {
            strategy.setAttemptsMade(0);
            logger.warning("Unable to reconnect: max reconnection attempts reached");
            return;
        }

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (strategy == null) {
                    logger.warning("Unable to reconnect: reconnection is null");
                    return;
                }
                strategy.processValues();
                Socket.this.connect();
                timer.cancel();
                timer.purge();
            }
        }, strategy.getReconnectInterval());
    }

    public void disconnect() {
        if (ws != null) {
            ws.disconnect();
        }
        strategy = null;
    }

    /**
     * States can be
     * CLOSED
     * CLOSING
     * CONNECTING
     * CREATED
     * OPEN
     */

    public WebSocketState getCurrentState() {
        return ws != null ? ws.getState() : null;
    }

    public Boolean isconnected() {
        return ws != null && ws.getState() == WebSocketState.OPEN;
    }

    public void disableLogging() {
        logger.setLevel(Level.OFF);
    }

    /**
     * Channels need to be subscribed everytime whenever client is reconnected to server (handled inside)
     * Add only one listener to one channel for whole lifetime of process
     */

    public class Channel {

        String channelName;

        public String getChannelName() {
            return channelName;
        }

        public Channel(String channelName) {
            this.channelName = channelName;
        }

        public void subscribe() {
            Socket.this.subscribe(channelName);
        }

        public void subscribe(Ack ack) {
            Socket.this.subscribe(channelName, ack);
        }

        public void onMessage(Listener listener) {
            Socket.this.onSubscribe(channelName, listener);
        }

        public void publish(Object data) {
            Socket.this.publish(channelName, data);
        }

        public void publish(Object data, Ack ack) {
            Socket.this.publish(channelName, data, ack);
        }

        public void unsubscribe() {
            Socket.this.unsubscribe(channelName);
            channels.remove(this);
        }

        public void unsubscribe(Ack ack) {
            Socket.this.unsubscribe(channelName, ack);
            channels.remove(this);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        ws.disconnect("Client socket garbage collected, closing connection");
        super.finalize();
    }
}
