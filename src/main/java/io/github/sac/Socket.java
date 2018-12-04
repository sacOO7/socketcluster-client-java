package io.github.sac;

import okhttp3.*;
import okio.ByteString;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by sachin on 7/6/17.
 */

public /*final*/ class Socket extends Emitter {

    private final Logger logger = Logger.getLogger(Socket.class.getName());

    private AtomicInteger counter;
    private BasicListener listener;
    private OkHttpClient client;
    private String url;
    private TaskHandler pingHandler;
    private TaskHandler timeoutHandler;
    private long pingInterval;
    private WebSocket ws;
    private State currentState = State.DISCONNECTED;

    private ReconnectionStrategy strategy;
    private Timer timer;
    private boolean selfDisconnect;
    private boolean pingEnable;
    private String AuthToken;
    private HashMap<Long, Object[]> acks;
    private ConcurrentHashMap<String, Channel> channels;
    private Headers headers;

    public Socket(String url) {
        this.url = url;
        this.client = new OkHttpClient();
        counter = new AtomicInteger(1);
        acks = new HashMap<>();
        channels = new ConcurrentHashMap<>();

        setState(State.DISCONNECTED);
        selfDisconnect = false;
        pingEnable = false;
        pingInterval = 2000;
        pingHandler = new TaskHandler();
        timeoutHandler = new TaskHandler();
        headers = getDefaultHeadersBuilder().build();
    }

    private Headers.Builder getDefaultHeadersBuilder() {
        return new Headers.Builder()
                .add("Accept-Encoding", "gzip, deflate, sdch")
                .add("Accept-Language", "en-US,en;q=0.8")
                .add("Pragma", "no-cache")
                .add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");
    }

    public Socket.Channel createChannel(String name) {
        if (channels.containsKey(name)) {
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
        if (channels.containsKey(name)) {
            return channels.get(name);
        }
        return null;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setReconnection(ReconnectionStrategy strategy) {
        this.strategy = strategy;
    }


    public void setListener(BasicListener listener) {
        this.listener = listener;
    }


    public Logger getLogger() {
        return logger;
    }

    public void setAuthToken(String token) {
        AuthToken = token;
    }

    public WebSocketListener getWebscoketListener() {
        return new WebSocketListener() {
            // OkHttp WebSocket callbacks
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("Connected to server");
                /**
                 * Code for sending handshake
                 */

                setState(State.CONNECTED);
                counter.set(1);

                if (strategy != null) {
                    strategy.setNumberOfAttempts(0);
                }
                try {
                    JSONObject handshakeObject = new JSONObject();
                    handshakeObject.put("event", "#handshake");
                    JSONObject object = new JSONObject();
                    object.put("authToken", AuthToken);
                    handshakeObject.put("data", object);
                    handshakeObject.put("cid", counter.getAndIncrement());
                    sendData(handshakeObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                listener.onConnected(Socket.this, response.headers());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    onTextMessage(text);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                try {
                    onTextMessage(bytes.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket closing: " + code + " - " + reason);
                setState(State.DISCONNECTING);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                setState(State.DISCONNECTED);
                logger.warning("Disconnected from server");
                pingHandler.removeLast();
                timeoutHandler.removeLast();
                processReconnection();
                listener.onDisconnected(Socket.this, code, reason);

            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
                logger.warning("Connect error: " + throwable);
                setState(State.DISCONNECTED);
                pingHandler.removeLast();
                timeoutHandler.removeLast();
                processReconnection();
                listener.onConnectError(Socket.this, throwable, response);
            }
        };
    }


    private void onTextMessage(String text) throws JSONException {
        logger.info("Receiving: " + text);


        // Valid message - reschedule next ping
        reschedulePing();

        // Proccess PING messages or send the message downstream

        if (text.equalsIgnoreCase(Constants.PING_MESSAGE)) {
            sendData(Constants.PONG_MESSAGE);
        } else {
            JSONObject object = new JSONObject(text);

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
                        listener.onAuthentication(this, ((JSONObject) dataobject).getBoolean("isAuthenticated"));
                        subscribeChannels();
                        break;
                    case PUBLISH:
                        handlePublish(((JSONObject) dataobject).getString("channel"), ((JSONObject) dataobject).opt("data"));
                        break;
                    case REMOVETOKEN:
                        setAuthToken(null);
                        break;
                    case SETTOKEN:
                        String token = ((JSONObject) dataobject).getString("token");
                        setAuthToken(token);
                        listener.onSetAuthToken(token, this);
                        break;
                    case EVENT:
                        if (hasEventAck(event)) {
                            handleEmitAck(event, dataobject, ack(Long.valueOf(cid)));
                        } else {
                            handleEmit(event, dataobject);

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
        }
    }

    public void setPingInterval(long pingInterval) {
        pingEnable = true;
        if (pingInterval != this.pingInterval) {
            this.pingInterval = pingInterval;
        }
    }

    public void disablePing() {
        if (pingEnable) {
            pingHandler.cancel();
            pingEnable = false;
        }
    }

    public void enablePing() {
        if (!pingEnable) {
            pingEnable = true;
            sendData(Constants.PING_MESSAGE);
        }
    }

    public boolean isPingEnabled() {
        return pingEnable;
    }

    private void setState(State state) {
        logger.info(String.format("setState: old %s, new %s", currentState.name(), state.name()));
        currentState = state;
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
                sendData(eventObject.toString());
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
                sendData(eventObject.toString());
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
                sendData(subscribeObject.toString());
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
                sendData(subscribeObject.toString());
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
                sendData(subscribeObject.toString());
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
                sendData(subscribeObject.toString());
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
                sendData(publishObject.toString());
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
                sendData(publishObject.toString());
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
                        sendData(object.toString());
                    }
                });
            }
        };
    }

    private void subscribeChannels() {
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            entry.getValue().subscribe();
        }
    }

    public void setExtraHeaders(Map<String, String> extraHeaders, boolean overrideDefaultHeaders) {
        Headers.Builder builder = new Headers.Builder();
        if (overrideDefaultHeaders) {
            builder = getDefaultHeadersBuilder();
        }
        for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        headers = builder.build();
    }

    public Headers getHeaders() {
        return headers;
    }

    protected void sendDataInBackground(String message) {
        sendData(message);
    }

    public void sendData(String message) {
        if (getState() == State.CONNECTED) {
            logger.info("Sending: " + message);
            ws.send(message);
        }
    }


    protected Request buildRequest(Headers headers) {
        setState(State.CREATED);
        // Create a WebSocket with a socket connection timeout value.
        return new Request.Builder()
                .url(url)
                .headers(headers)
                .build();
    }

    private void connect() {
        Request request = buildRequest(headers);
        setState(State.CONNECTING);
        ws = client.newWebSocket(request, getWebscoketListener());
    }

    public void connectAsync() {
        connect();
    }

    public void reconnect() {
        logger.info("reconnecting");
        connect();
    }


    public void disconnect() {
        disconnect("close");
    }

    public void disconnect(String reason) {
        logger.info("Calling disconnect");
        if (currentState == State.DISCONNECTED) {
            return;
        } else if (currentState == State.CONNECTED) {
            ws.close(1001, reason);
            setState(State.DISCONNECTING);
        } else {
            setState(State.DISCONNECTED);
        }

        pingHandler.removeLast();
        timeoutHandler.removeLast();
        selfDisconnect = true;
    }


    /* visible for testing */
    void processReconnection() {
        if (strategy != null && !selfDisconnect) {
            if (strategy.getNumberOfAttempts() < strategy.getMaxAttempts()) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        reconnect();
                        strategy.processAttempts();
                        timer.cancel();
                        timer.purge();
                    }
                }, strategy.getReconnectInterval());

            } else {
                pingHandler.cancel();
                logger.info("Number of attempts are complete");
            }
        } else {
            pingHandler.cancel();
            selfDisconnect = false;
        }
    }

    // TODO: 15/8/17 solve problem of PONG RECEIVE FAILED by giving a fair chance
    protected void reschedulePing() {
        if (!pingEnable)
            return;

        logger.info("Scheduling ping in: " + pingInterval + " ms");
        pingHandler.removeLast();
        timeoutHandler.removeLast();
        pingHandler.postDelayed(new TimerTask() {
            @Override
            public void run() {
                logger.info("SENDING PING");
                sendData(Constants.PING_MESSAGE);
            }
        }, pingInterval);
        timeoutHandler.postDelayed(new TimerTask() {
            @Override
            public void run() {
                if (getState() != State.DISCONNECTING && getState() != State.DISCONNECTED) {
                    logger.warning("PONG RECEIVE FAILED");
                    ws.cancel();
                    //onFailure(ws, new IOException("PING Timeout"), null);
                }
                timeoutHandler.removeLast();
            }
        }, 2 * pingInterval);
    }


    public State getState() {
        return currentState;
    }

    public Boolean isconnected() {
        return getState() == State.CONNECTED;
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
        disconnect("Client socket garbage collected, closing connection");
        super.finalize();
    }

}

