import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import io.github.sac.*;

import java.util.List;
import java.util.Map;

/**
 * Created by sachin on 8/11/16.
 */

public class Main {

    public static String url="ws://localhost:8000/socketcluster/";

    public static void main(String arg[]) {

        Socket socket = new Socket(url);

        socket.setListener(new BasicListener() {

            public void onConnected(Socket socket,Map<String, List<String>> headers) {
                System.out.println("Connected to endpoint");
            }

            public void onDisconnected(Socket socket,WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                System.out.println("Disconnected from end-point");
            }

            public void onConnectError(Socket socket,WebSocketException exception) {
                System.out.println("Got connect error "+ exception);
            }

            public void onSetAuthToken(String token, Socket socket) {
                System.out.println("Set auth token got called");
                socket.setAuthToken(token);
            }

            public void onAuthentication(Socket socket,Boolean status) {
                if (status) {
                    System.out.println("socket is authenticated");
                } else {
                    System.out.println("Authentication is required (optional)");
                }
            }

        });

        socket.setReconnection(new ReconnectStrategy().setDelay(3000).setMaxAttempts(10)); //Connect after each 2 seconds for 30 times


                socket.connect();


        socket.disableLogging();


                socket.emit("chat","Hi");
        socket.emit("chat", "Hi", new Ack() {
            @Override
            public void call(String eventName, Object error, Object data) {
                System.out.println("Got message for :"+eventName+" error is :"+error+" data is :"+data);
            }
        });

        socket.on("yell", new Emitter.Listener() {
            @Override
            public void call(String eventName, Object data) {
                System.out.println("Got message for :"+eventName+" data is :"+data);
            }
        });

        socket.on("yell", new Emitter.AckListener() {
            @Override
            public void call(String eventName, Object data, Ack ack) {
                System.out.println("Got message for :"+eventName+" data is :"+data);
                //sending ack back

                ack.call(eventName,"This is error","This is data");
            }
        });
//
//
        Socket.Channel channel = socket.createChannel("yell");
//
        channel.subscribe(new Ack() {
            @Override
            public void call(String channelName, Object error, Object data) {
                if (error==null){
                    System.out.println("Subscribed to channel "+channelName+" successfully");
                }
            }
        });

        channel.publish("Hi sachin", new Ack() {
            @Override
            public void call(String channelName, Object error, Object data) {
                if (error==null){
                    System.out.println("Published message to channel "+channelName+" successfully");
                }
            }
        });

        channel.onMessage(new Emitter.Listener() {
            @Override
            public void call(String channelName, Object data) {

                System.out.println("Got message for channel "+channelName+" data is "+data);
            }
        });

        channel.unsubscribe(new Ack() {
            @Override
            public void call(String name, Object error, Object data) {
                System.out.println("Unsubscribed successfully");
            }
        });
        channel.unsubscribe();

//        channel.subscribe(new Ack() {
//                              @Override
//                              public void call(String name, Object error, Object data) {
//
//                              }
//          });
////
//        channel.onMessage(new Emitter.Listener() {
//            public void call(Object object) {
//                System.out.println("got message " + object);
//            }
//        });

//
//
//
//        socket.on("chat", new Emitter.Listener() {
//            public void call(Object object) {
//                System.out.println("Got echo event :: " + object);
//            }
//        });
//
//
//
//        socket.emit("chat", "hi", new Ack() {
//            public void call(Object error, Object data) {
//
//            }
//        });
//
//
//
//        while (true) {
//            Scanner scanner = new Scanner(System.in);
//
//            channel.publish(scanner.nextLine(), new Ack() {
//                public void call(Object error, Object data) {
//                    if (error == null) {
//                        System.out.println("Publish sent successfully");
//                    }
//                }
//            });
//        }

    }
}
