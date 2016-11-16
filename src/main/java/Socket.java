import com.neovisionaries.ws.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sachin on 13/11/16.
 */

public class Socket extends Emitter{

    AtomicInteger counter;
    String URL;
    WebSocketFactory factory;
    WebSocket ws;
    BasicListener listener;
    String AuthToken;
    HashMap <Long,Ack> acks;

    public Socket(String URL) {
        this.URL = URL;
        factory=new WebSocketFactory().setConnectionTimeout(5000);
        counter=new AtomicInteger(1);
        acks=new HashMap<Long, Ack>();
    }

    public void seturl(String url){
        this.URL=url;
    }

    public void setListener(BasicListener listener){
        this.listener=listener;
    }

    public WebSocket getInnnerWebsocketInstance(){
        return ws;
    }

    public void setAuthToken(String token){
        AuthToken=token;
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

    public Socket subscribe(final String channel, Listener listener){
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
        this.on(channel,listener);
        return this;
    }

    public Socket ackSubscribe(final String channel, Listener listener, final Ack ack){
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
        this.on(channel,listener);
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

    public Socket ackPublish (final String channel, final Object data,final  Ack ack){
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



    public Emitter on(String event, Listener fn , Ack ack) {

        return super.on(event, fn);
    }

    public void connect() throws IOException {

        ws=factory.createSocket("ws://localhost:8000/socketcluster/");
        ws.addExtension("permessage-deflate; client_max_window_bits");
        ws.addHeader("Accept-Encoding","gzip, deflate, sdch");
        ws.addHeader("Accept-Language","en-US,en;q=0.8");
        ws.addHeader("Pragma","no-cache");
        ws.addHeader("User-Agent","Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36");

        ws.addListener(new WebSocketAdapter(){


            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {

                /**
                 * Code for sending handshake
                 */

                counter.set(1);

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
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                listener.onConnectError(exception);
                super.onConnectError(websocket, exception);
            }

            @Override
            public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
                System.out.println("New state is "+newState.name());
                super.onStateChanged(websocket, newState);
            }

            @Override
            public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("Ping frame received");
                super.onPingFrame(websocket, frame);
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
                            break;
                        case PUBLISH:
                            Socket.this.handleEmit(String.valueOf((((JSONObject)dataobject).get("channel"))), ((JSONObject)dataobject).get("data"));
                            break;
                        case REMOVETOKEN:
                            setAuthToken(null);
                            break;
                        case SETTOKEN:
                            listener.onSetAuthToken((String) ((JSONObject)dataobject).get("token"));
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
            @Override
            public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On continuation frame got called");
                super.onContinuationFrame(websocket, frame);
            }

            @Override
            public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
//                System.out.println("On text frame got called :"+frame.getPayloadText());
                super.onTextFrame(websocket, frame);
            }

            @Override
            public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On binary frame got called");
                super.onBinaryFrame(websocket, frame);
            }

            @Override
            public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On close frame got called");
                super.onCloseFrame(websocket, frame);
            }

            @Override
            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On pong frame got called");
                super.onPongFrame(websocket, frame);
            }

            @Override
            public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
                System.out.println("On binary frame got called");
                super.onBinaryMessage(websocket, binary);
            }


            @Override
            public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
                System.out.println("On frame unsent got called");
                super.onFrameUnsent(websocket, frame);
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                System.out.println("On error got called");
                super.onError(websocket, cause);
            }

            @Override
            public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                System.out.println("Got onframe error");
                super.onFrameError(websocket, cause, frame);
            }

            @Override
            public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
                System.out.println("Got message error");
                super.onMessageError(websocket, cause, frames);
            }

            @Override
            public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
                System.out.println("Got message decompression error");
                super.onMessageDecompressionError(websocket, cause, compressed);
            }

            @Override
            public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
                System.out.println("Got text message error");
                super.onTextMessageError(websocket, cause, data);
            }

            @Override
            public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                System.out.println("Got send error");
                super.onSendError(websocket, cause, frame);
            }

            @Override
            public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
                System.out.println("Got unexpected error");
                super.onUnexpectedError(websocket, cause);
            }

        });

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

            System.out.println("Failed to establish a WebSocket connection "+e.getError());
        }
    }


    public void disconnect(){
        ws.disconnect();
    }

}
