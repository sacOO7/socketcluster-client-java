import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.*;

/**
 * Created by sachin on 8/11/16.
 */

public class Main {

    public static String url="ws://localhost:8000/socketcluster/";

    public static void main(String arg[]) throws IOException {

        final Socket socket=new Socket(url);
        socket.setListener(new BasicListener() {

            public void onConnected(Map<String, List<String>> headers) {
                System.out.println("Connected to endpoint");
            }

            public void onDisconnected(WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                System.out.println("Disconnected from end-point");
            }

            public void onConnectError(WebSocketException exception) {
                System.out.println("Got connect error");
            }

            public void onSetAuthToken(String token) {
                socket.setAuthToken(token);
            }

            public void onAuthentication(Boolean status) {
                if (status){
                    System.out.println("Socket is authenticated");
                }else{
                    System.out.println("Authentication is required (optional)");
                }
            }

        });

        socket.connect();

        socket.subscribe("yell", new Emitter.Listener() {
            public void call(Object... args) {
                System.out.println("Got publish :: " +args[0]);
            }
        });

        socket.on("chat", new Emitter.Listener() {
            public void call(Object... args) {
                System.out.println("Got echo event :: " +args[0]);
            }
        });


        while (true) {
            Scanner scanner=new Scanner(System.in);
//            socket.send("chat",scanner.nextLine());
            socket.publish("yell",scanner.nextLine());
        }

//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                socket.disconnect();
//            }
//        },2000);
//
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                try {
//                    socket.connect();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        },4000);
    }

}
