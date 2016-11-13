import com.neovisionaries.ws.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by sachin on 13/11/16.
 */

public class Socket extends Emitter{

    int cid=1;
    String URL;
    WebSocketFactory factory;
    WebSocket ws;
    BasicListener listener;

    public Socket(String URL) {
        this.URL = URL;
        factory=new WebSocketFactory().setConnectionTimeout(5000);
    }

    public void seturl(String url){
        this.URL=url;
    }

    public void setListener(BasicListener listener){
        this.listener=listener;
    }

    public Socket send(String event,JSONObject object){
        JSONObject eventObject=new JSONObject();
        eventObject.put("event",event);
        eventObject.put("data",object);
        eventObject.put("cid",cid++);
        ws.sendText(eventObject.toJSONString());
//        socket.sendText("{\"event\":\""+eventname+"\",\"data\":\""+data+"\",\"cid\":"+ cid++ +"}");
        return this;
    }

    public Socket send(String event,String data){
        ws.sendText("{\"event\":\""+event+"\",\"data\":\""+data+"\",\"cid\":"+ cid++ +"}");
        return this;
    }

    public Socket subscribe(String channel,Listener listener){

        JSONObject subscribeObject=new JSONObject();
        subscribeObject.put("event","#subscribe");
        JSONObject object=new JSONObject();
        object.put("channel",channel);
        subscribeObject.put("data",object);
        subscribeObject.put("cid",cid++);
        ws.sendText(subscribeObject.toJSONString());
//        ws.sendText("{\"event\":\"#subscribe\",\"data\":{\"channel\":\""+channel+"\"},\"cid\":"+cid++ +"}");
        this.on(channel,listener);
        return this;
    }

    public Socket publish (String channel,Object data){
        JSONObject publishObject=new JSONObject();
        publishObject.put("event","#publish");
        JSONObject object=new JSONObject();
        object.put("channel",channel);
        object.put("data",data);
        publishObject.put("data",object);
        ws.sendText(publishObject.toJSONString());

//        ws.sendText("{\"event\":\"#publish\",\"data\":{\"channel\":\""+channel+"\",\"data\":\""+data+"\"}}");
        return this;
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

                JSONObject object=new JSONObject();
                object.put("event","#handshake");
                object.put("data",new JSONObject().put("authToken",null));
                object.put("cid",cid++);
                websocket.sendText(object.toJSONString());

//                websocket.sendText("{\"event\": \"#handshake\",\"data\": {\"authToken\":null},\"cid\": "+ cid++ +"}");

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


                    if (((String)object.get("event")).equals("#publish")){

                        JSONObject dataobject=(JSONObject)object.get("data");
                        Socket.this.emit(String.valueOf((dataobject.get("channel"))),dataobject.get("data"));

                    }else{
                        Socket.this.emit((String) object.get("event"),object.get("data"));
                    }

                }
                super.onFrame(websocket, frame);
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

}
