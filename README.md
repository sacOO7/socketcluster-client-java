Java and Android Socketcluster Client
=====================================
Overview
--------
This client provides following functionality

- Support for emitting and listening to remote events
- Automatic reconnection
- Pub/sub
- Authentication (JWT)

Description
-----------
Create instance of `Socket` class by passing url of socketcluster-server end-point

```java
    //Create a socket instance
    String url="ws://localhost:8000/socketcluster/";
    Socket socket = new Socket(url);
    
```


#### Registering basic listeners
 
Implemented using BasicListener interface

```java
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
                    System.out.println("Socket is authenticated");
                } else {
                    System.out.println("Authentication is required (optional)");
                }
            }

        });
```

#### Connecting to server

- For connecting to a server use :

```java
    //This will send websocket handshake request to socketcluster-server
    socket.connect();
```

- By default reconnection to a server is not enabled , to enable it :

```java
    //This will set automatic-reconnection to server with delay of 2 seconds and repeating it for 30 times
    socket.setReconnection(new ReconnectStrategy().setMaxAttempts(30));
    socket.connect();
```

- To disable reconnection :

```
   socket.setReconnection(null); 
```

#### Emitting and listening to events

###### Event emitter

- Eventname is name of event to be sent to server and message can be a String ,boolean ,Long or JSON-object

```java
    socket.emit(eventname,message);
    
    //socket.emit("chat","Hi");
```

- To send event with acknowledgement for handling errors

```java
    socket.emit(eventname, message, new Ack() {
                public void call(Object error, Object data) {
                    
                }
        });
```

###### Event Listener

- For listening to this event use :

The object received can be String , Boolean , Long or JSONObject.

```java
    socket.on(eventname, new Emitter.Listener() {
                public void call(Object object) {
                    
                    // Cast object to its proper datatype
                     
                    System.out.println("Got message :: " + object);
                }
        }); 
```

- To send acknowledgement back to server

```java
    socket.on(eventname, new Emitter.AckListener() {
            public void call(Object object, Ack ack) {
                
                // Cast object to its proper datatype                     
                System.out.println("Got message :: " + object);
                
                /...
                    Some logic goes here
                .../
                if (error){
                
                ack.call(error,null);
                
                }else{
                
                //Data can be of any data type
                
                ack.call(null,data);
                }
                
                
                //Both error and data can be sent to server
                
                //ack.call(error,data);
                
            }
        });
        
```

#### Implementing Pub-Sub via channels

###### Creating channel

- For creating and subscribing to channels:

```java
    Socket.Channel channel = socket.createChannel(channelName);
    //Socket.Channel channel = socket.createChannel("yolo"); 
    
    channel.subscribe(new Ack() {
                public void call(Object error, Object data) {
                    if (error == null) {
                        System.out.println("subscibed to channel successfully");
                    }
                }
        });
```

- For getting list of created channels :
 
```java
    List <Socket.Channel> channels=socket.getChannels();
``` 

- To get channel by name :

```java
        Socket.Channel channel=socket.getChannelByName("yolo");
        //Returns null if channel of given name is not present
        
```




###### Publishing event on channel

- For publishing event :

```java
       // message can have any data type
       
       channel.publish(message, new Ack() {
                public void call(Object error, Object data) {
                    if (error == null) {
                        System.out.println("Publish sent successfully");
                    }
                }
        });
        
``` 
 
###### Listening to channel

- For listening to channel event :

```java
    channel.onMessage(new Emitter.Listener() {
             public void call(Object object) {
                 System.out.println("got message " + object);
             }
         });
``` 
 
<!--###### Pub-sub without creating channel-->
 
#### Handling SSL connection with server
 
`WebSocketFactory` class is responsible for creating websocket instances and handling settings with server.For more
information visit [link](https://github.com/TakahikoKawasaki/nv-websocket-client/blob/master/README.md)

To get instance of `WebSocketFactory` class :

```java
   
    WebSocketFactory factory=socket.getFactorySettings();
    
```
 
The following is an example to set a custom SSL context to a `WebSocketFactory`
instance. (Again, you don't have to call a `setSSL*` method if you use the default
SSL configuration.)

```java
// Create a custom SSL context.
SSLContext context = NaiveSSLContext.getInstance("TLS");

// Set the custom SSL context.
factory.setSSLContext(context);
```

[NaiveSSLContext](https://gist.github.com/TakahikoKawasaki/d07de2218b4b81bf65ac)
used in the above example is a factory class to create an `SSLContext` which
naively accepts all certificates without verification. It's enough for testing
purposes. When you see an error message "unable to find valid certificate path
to requested target" while testing, try `NaiveSSLContext`. 
 
 
#### Setting HTTP proxy with server
 
If a WebSocket endpoint needs to be accessed via an HTTP proxy, information
about the proxy server has to be set to a `WebSocketFactory` instance before
creating a `WebSocket` instance. Proxy settings are represented by
`ProxySettings` class. A `WebSocketFactory` instance has an associated
`ProxySettings` instance and it can be obtained by calling
`WebSocketFactory.getProxySettings()` method.

```java
// Get the associated ProxySettings instance.
ProxySettings settings = factory.getProxySettings();
```

`ProxySettings` class has methods to set information about a proxy server such
as `setHost` method and `setPort` method. The following is an example to set a
secure (`https`) proxy server.

```java
// Set a proxy server.
settings.setServer("https://proxy.example.com");
```

If credentials are required for authentication at a proxy server, `setId`
method and `setPassword` method, or `setCredentials` method can be used to set
the credentials. Note that, however, the current implementation supports only
Basic Authentication.

```java
// Set credentials for authentication at a proxy server.
settings.setCredentials(id, password);
``` 
 