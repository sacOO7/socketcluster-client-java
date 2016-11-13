import com.neovisionaries.ws.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by sachin on 8/11/16.
 */

public class Main {
    public static String url="ws://localhost:8000/socketcluster/";

    public static void main(String arg[]) throws IOException {

        Socket socket=new Socket(url);
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

    }

}
