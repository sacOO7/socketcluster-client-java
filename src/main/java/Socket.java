import com.neovisionaries.ws.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sachin on 13/11/16.
 */

public class Socket extends Emitter{

    private AtomicInteger counter;
    private String URL;
    private WebSocketFactory factory;
    private ReconnectStrategy strategy;
    private WebSocket ws;
    private BasicListener listener;
    private String AuthToken;
    private HashMap <Long,Ack> acks;
    private List <Channel> channels;
    private WebSocketAdapter adapter;

    public Socket(String URL) {
        this.URL = URL;
        factory=new WebSocketFactory().setConnectionTimeout(5000);
        counter=new AtomicInteger(1);
        acks=new HashMap<Long, Ack>();
        channels=new ArrayList<Channel>();
        adapter=getAdapter();
    }

    public Channel createChannel(String name){
        Channel channel=new Channel(name);
        channels.add(channel);
        return channel;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public Channel getChannelByName(String name){
        for (Channel channel:channels){
            if (channel.getChannelName().equals(name))
                return channel;
        }
        return null;
    }

    public void seturl(String url){
        this.URL=url;
    }

    public void setReconnection(ReconnectStrategy strategy) {
        this.strategy = strategy;
    }

    public void setListener(BasicListener listener){
        this.listener=listener;
    }

    /**
     * used to set up TLS/SSL connection to server for more details visit neovisionaries websocket client
     * @return
     */

    public WebSocketFactory getFactorySettings(){
        return factory;
    }

    public void setAuthToken(String token){
        AuthToken=token;
    }

    public WebSocketAdapter getAdapter(){
        return new WebSocketAdapter(){

            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {

                /**
                 * Code for sending handshake
                 */

                counter.set(1);
                if (strategy!=null)
                strategy.setAttmptsMade(0);

                JSONObject handshakeObject=new JSONObject();
                handshakeObject.put("event","#handshake");
                JSONObject object=new JSONObject();
                object.put("authToken",AuthToken);
                handshakeObject.put("data",object);
                handshakeObject.put("cid",counter.getAndIncrement());
                websocket.sendText(handshakeObject.toJSONString());

//                websocket.sendText("{\"event\": \"#handshake\",\"data\": {\"authToken\":\""+AuthToken+"\"},\"cid\": "+ cid++ +"}");

                listener.onConnected(headers);

                super.onConnected(websocket, headers);
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                listener.onDisconnected(serverCloseFrame,clientCloseFrame,closedByServer);
                if (strategy!=null) {
                    reconnect();
                }else{
                    System.out.println("cant reconnect , reconnection is null");
                }
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                listener.onConnectError(exception);
                if (strategy!=null) {
                    reconnect();
                }else{
                    System.out.println("cant reconnect , reconnection is null");
                }
                super.onConnectError(websocket, exception);
            }


            @Override
            public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
//                System.out.println("On frame got called :"+frame.getPayloadText());


                if (frame.getPayloadText().equalsIgnoreCase("#1")) {
                    /**
                     *  PING-PONG logic goes here
                     */
                    websocket.sendText("#2");
                }else {
                    JSONParser parser = new JSONParser();
                    JSONObject object= (JSONObject) parser.parse(frame.getPayloadText());

                    /**
                     * Message retrieval mechanism goes here
                     */
//                    System.out.println("new message :"+object.toJSONString());

                    Object dataobject = object.get("data");
                    Long rid = (Long) object.get("rid");
                    Long cid = (Long) object.get("cid");
                    String event = (String) object.get("event");


                    switch (Parser.parse(dataobject,rid,cid,event)) {

                        case ISAUTHENTICATED:
                            listener.onAuthentication((Boolean) ((JSONObject)dataobject).get("isAuthenticated"));
                            subscribeChannels();
                            break;
                        case PUBLISH:
                            Socket.this.handleEmit(String.valueOf((((JSONObject)dataobject).get("channel"))), ((JSONObject)dataobject).get("data"));
                            break;
                        case REMOVETOKEN:
                            setAuthToken(null);
                            break;
                        case SETTOKEN:
                            listener.onSetAuthToken((String) ((JSONObject)dataobject).get("token"),Socket.this);
                            break;
                        case EVENT:
                            if (hasEventAck(event)) {
                                System.out.println("This event has ack");
                                handleEmitAck(event,dataobject,ack(cid));
                            }else {
                                Socket.this.handleEmit(event, dataobject);
                                System.out.println("This ack doesnt have ack");
                            }
                            break;
                        case ACKRECEIVE:

                            if (acks.containsKey(rid)){
//                                System.out.println("Contains ack with id "+rid);
                                Ack fn=acks.remove(rid);
                                if (fn!=null){
//                                    System.out.println("calling fun with ack"+rid);
                                    fn.call(object.get("error"),object.get("data"));
                                }else{
                                    System.out.println("ack function is null with rid "+rid);
                                }
                            }
                            break;
                    }

                }
                super.onFrame(websocket, frame);
            }


            @Override
            public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On close frame got called");
                super.onCloseFrame(websocket, frame);
            }

            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                System.out.println("Got send error");
                super.onSendError(websocket, cause, frame);
            }

        };

    }
    public Socket emit(final String event, final Object object){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject eventObject=new JSONObject();
                eventObject.put("event",event);
                eventObject.put("data",object);
                eventObject.put("cid",counter.getAndIncrement());
                ws.sendText(eventObject.toJSONString());
            }
        });
//        socket.sendText("{\"event\":\""+eventname+"\",\"data\":\""+data+"\",\"cid\":"+ cid++ +"}");
        return this;
    }

    public Socket emit(final String event, final Object object, final Ack ack){

        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject eventObject=new JSONObject();
                acks.put(counter.longValue(),ack);
                eventObject.put("event",event);
                eventObject.put("data",object);
                eventObject.put("cid",counter.getAndIncrement());
                ws.sendText(eventObject.toJSONString());
            }
        });
        return this;
    }

    public Socket subscribe(final String channel){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject=new JSONObject();
                subscribeObject.put("event","#subscribe");
                JSONObject object=new JSONObject();
                object.put("channel",channel);
                subscribeObject.put("data",object);

                subscribeObject.put("cid",counter.getAndIncrement());
                ws.sendText(subscribeObject.toJSONString());
            }
        });
//        ws.sendText("{\"event\":\"#subscribe\",\"data\":{\"channel\":\""+channel+"\"},\"cid\":"+cid++ +"}");
        return this;
    }

    public Socket subscribe(final String channel, final Ack ack){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject=new JSONObject();
                subscribeObject.put("event","#subscribe");
                JSONObject object=new JSONObject();
                acks.put(counter.longValue(),ack);

                object.put("channel",channel);
                subscribeObject.put("data",object);
                subscribeObject.put("cid",counter.getAndIncrement());
                ws.sendText(subscribeObject.toJSONString());
            }
        });
//        ws.sendText("{\"event\":\"#subscribe\",\"data\":{\"channel\":\""+channel+"\"},\"cid\":"+cid++ +"}");
        return this;
    }

    public Socket unsubscribe(final String channel){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject subscribeObject=new JSONObject();
                subscribeObject.put("event","#unsubscribe");
                JSONObject object=new JSONObject();
                object.put("channel",channel);
                subscribeObject.put("data",object);
                subscribeObject.put("cid",counter.getAndIncrement());
                ws.sendText(subscribeObject.toJSONString());
            }
        });
        return this;
    }

    public Socket publish (final String channel, final Object data){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject publishObject=new JSONObject();
                publishObject.put("event","#publish");
                JSONObject object=new JSONObject();
                object.put("channel",channel);
                object.put("data",data);
                publishObject.put("data",object);
                publishObject.put("cid",counter.getAndIncrement());
                ws.sendText(publishObject.toJSONString());
            }
        });

//        ws.sendText("{\"event\":\"#publish\",\"data\":{\"channel\":\""+channel+"\",\"data\":\""+data+"\"}}");
        return this;
    }

    public Socket publish (final String channel, final Object data,final  Ack ack){
        EventThread.exec(new Runnable() {
            public void run() {
                JSONObject publishObject=new JSONObject();
                publishObject.put("event","#publish");
                JSONObject object=new JSONObject();
                acks.put(counter.longValue(),ack);
                object.put("channel",channel);
                object.put("data",data);
                publishObject.put("data",object);
                publishObject.put("cid",counter.getAndIncrement());
                ws.sendText(publishObject.toJSONString());
            }
        });

//        ws.sendText("{\"event\":\"#publish\",\"data\":{\"channel\":\""+channel+"\",\"data\":\""+data+"\"}}");
        return this;
    }

    public Ack ack(final Long cid){
        return new Ack() {
            public void call(final Object error, final Object data) {
                EventThread.exec(new Runnable() {
                    public void run() {
                        JSONObject object=new JSONObject();
                        object.put("error",error);
                        object.put("data",data);
                        object.put("rid",cid);
                        ws.sendText(object.toJSONString());
                    }
                });
            }
        };
    }


    private void subscribeChannels(){
        for (Channel channel:channels){
            channel.subscribe();
        }
    }

    public void connect() {

        try {
            ws = factory.createSocket("ws://localhost:8000/socketcluster/");
        }catch (IOException e){
            System.out.printf(e.getMessage());
        }
        ws.addExtension("permessage-deflate; client_max_window_bits");
        ws.addHeader("Accept-Encoding","gzip, deflate, sdch");
        ws.addHeader("Accept-Language","en-US,en;q=0.8");
        ws.addHeader("Pragma","no-cache");
        ws.addHeader("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");

        ws.addListener(adapter);

        try {
            ws.connect();
        }catch (OpeningHandshakeException e)
        {
            // A violation against the WebSocket protocol was detected
            // during the opening handshake.

            // Status line.
            StatusLine sl = e.getStatusLine();
            System.out.println("=== Status Line ===");
            System.out.format("HTTP Version  = %s\n", sl.getHttpVersion());
            System.out.format("Status Code   = %d\n", sl.getStatusCode());
            System.out.format("Reason Phrase = %s\n", sl.getReasonPhrase());

            // HTTP headers.
            Map<String, List<String>> headers = e.getHeaders();
            System.out.println("=== HTTP Headers ===");
            for (Map.Entry<String, List<String>> entry : headers.entrySet())
            {
                // Header name.
                String name = entry.getKey();

                // Values of the header.
                List<String> values = entry.getValue();

                if (values == null || values.size() == 0)
                {
                    // Print the name only.
                    System.out.println(name);
                    continue;
                }

                for (String value : values)
                {
                    // Print the name and the value.
                    System.out.format("%s: %s\n", name, value);
                }
            }
        }
        catch (WebSocketException e)
        {
            // Failed to establish a WebSocket connection.
            listener.onConnectError(e);
            if (strategy!=null) {
                reconnect();
            }else{
                System.out.println("cant reconnect , reconnection is null");
            }
        }

    }


    public void reconnect(){

//        if (!strategy.areAttemptsComplete()) {
//            strategy.setListener(new ReconnectStrategy.Callback() {
//                public void connect()  {
//                    System.out.println("reconnect got called");
//                    Socket.this.connect();
//                }
//            });
//        }else{
//            System.out.println("Attempts are complete");
//        }

        if (!strategy.areAttemptsComplete()) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    strategy.processValues();
                    Socket.this.connect();
                }
            },strategy.getReconnectInterval());
        }
    }

    public void disconnect(){
        ws.disconnect();
    }

    /**
     * Channels need to be subscribed everytime whenever client is reconnected to server (handled inside)
     * Add only one listener to one channel for whole lifetime of process
     */

    class Channel{

        String channelName;

        public String getChannelName() {
            return channelName;
        }

        public Channel(String channelName) {
            this.channelName = channelName;
        }

        public void subscribe(){
            Socket.this.subscribe(channelName);
        }

        public void subscribe(Ack ack){
            Socket.this.subscribe(channelName,ack);
        }

        public void onMessage(Listener listener){
            Socket.this.on(channelName,listener);
        }

        public void publish(Object data){
            Socket.this.publish(channelName,data);
        }

        public void publish(Object data,Ack ack){
            Socket.this.publish(channelName,data,ack);
        }

        public void unsubscribe(){
            Socket.this.unsubscribe(channelName);
            channels.remove(getChannelByName(channelName));
        }
    }
}
