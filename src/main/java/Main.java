import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import io.github.sac.*;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by sachin on 8/11/16.
 */

public class Main {

    public static String url="ws://localhost:8000/socketcluster/";

    public static void main(String arg[]) {

        Socket socket = new Socket(url);

        socket.setListener(new BasicListener() {

            public void onConnected(Map<String, List<String>> headers) {
                System.out.println("Connected to endpoint");
            }

            public void onDisconnected(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                System.out.println("Disconnected from end-point");
            }

            public void onConnectError(WebSocketException exception) {
                System.out.println("Got connect error "+ exception);
            }

            public void onSetAuthToken(String token, Socket socket) {
                socket.setAuthToken(token);
            }

            public void onAuthentication(Boolean status) {
                if (status) {
                    System.out.println("socket is authenticated");
                } else {
                    System.out.println("Authentication is required (optional)");
                }
            }

        });

        socket.setReconnection(new ReconnectStrategy().setDelay(2).setMaxAttempts(30)); //Connect after each 2 seconds for 30 times

        socket.connect();

//        socket.disableLogging();


//
//        Socket.Channel channel = socket.createChannel("yell");
//
//
//        channel.subscribe(new Ack() {
//            public void call(Object error, Object data) {
//                if (error == null) {
//                    System.out.println("channel sub success");
//                }
//            }
//        });
//
//        channel.onMessage(new Emitter.Listener() {
//            public void call(Object object) {
//                System.out.println("got message " + object);
//            }
//        });
//
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
