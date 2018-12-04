import io.github.sac.*;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by sachin on 8/11/16.
 */

public class Main {

    public static String url="ws://localhost:8000/socketcluster/";

    public static void main(String arg[]) {

        Socket socket = new Socket(url);

        socket.setListener(new BasicListener() {


            public void onSetAuthToken(String token, Socket socket) {
                System.out.println("Set auth token got called");
                socket.setAuthToken(token);
            }

            @Override
            public void onConnected(Socket socket, Headers headers) {
                System.out.println("Connected to endpoint");


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
                        System.out.println("Unsubscribed successfully for channel " + name);
                    }
                });
//                channel.unsubscribe();
            }

            @Override
            public void onDisconnected(Socket socket, int code, String reason) {
                System.out.println("Disconnected from end-point");
            }

            @Override
            public void onConnectError(Socket socket, Throwable throwable, Response response) {
                System.out.println("Got connect error "+  throwable.getMessage());
            }

            public void onAuthentication(Socket socket, Boolean status) {
                if (status) {
                    System.out.println("socket is authenticated");
                } else {
                    System.out.println("Authentication is required (optional)");
                }
            }

        });

        socket.setReconnection(new ReconnectionStrategy(10, 3000)); //Connect after each 2 seconds for 30 times


        socket.connectAsync();


        socket.disableLogging();



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
