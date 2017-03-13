Java and Android Socketcluster Client
=====================================

Overview
--------
This client provides following functionality

- Support for emitting and listening to remote events
- Automatic reconnection
- Pub/sub
- Authentication (JWT)

License
-------
Apache License, Version 2.0

Gradle
------
For java 

```Gradle
dependencies {
    compile 'io.github.sac:SocketclusterClientJava:1.7.2'
}
```
for sample java examples visit [Java Demo](https://github.com/sacOO7/socketcluster-client-testing/tree/master/src/main/java)

For android 

```Gradle
compile ('io.github.sac:SocketclusterClientJava:1.7.2'){
        exclude group :'org.json', module: 'json'
}
```
for sample android demo visit [Android Demo](https://github.com/sacOO7/socketcluster-android-demo)


[ ![Download](https://api.bintray.com/packages/sacoo7/Maven/socketcluster-client/images/download.svg) ](https://bintray.com/sacoo7/Maven/socketcluster-client/_latestVersion)

<!---
Download [latest jar dependency](https://github.com/sacOO7/socketcluster-client-java/blob/master/out/artifacts/SocketclusterClientJava_main_jar/SocketclusterClientJava_main.jar?raw=true)
-->

Description
-----------
Create instance of `Socket` class by passing url of socketcluster-server end-point

```java
    //Create a socket instance
    String url="ws://localhost:8000/socketcluster/";
    Socket socket = new Socket(url);
     
```
**Important Note** : Default url to socketcluster end-point is always *ws://somedomainname.com/socketcluster/*.


#### Registering basic listeners
 
Implemented using `BasicListener` interface

```java
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
```

#### Connecting to server

- For connecting to server:

```java
    //This will send websocket handshake request to socketcluster-server
    socket.connect();
```

- By default reconnection to server is not enabled , to enable it :

```java
    //This will set automatic-reconnection to server with delay of 2 seconds and repeating it for 30 times
    socket.setReconnection(new ReconnectStrategy().setDelay(2000).setMaxAttempts(30));
    socket.connect();
```

- To disable reconnection :

```
   socket.setReconnection(null); 
```

- By default logging of messages is enabled ,to disable :

```
   socket.disableLogging();
```

Emitting and listening to events
--------------------------------
#### Event emitter

- eventname is name of event and message can be String, boolean, Long or JSON-object

```java
    socket.emit(eventname,message);
    
    //socket.emit("chat","Hi");
```

- To send event with acknowledgement

```java
    socket.emit(eventname, message, new Ack() {
                public void call(String eventName,Object error, Object data) {
                    //If error and data is String
                    System.out.println("Got message for :"+eventName+" error is :"+error+" data is :"+data);
                }
        });
```

#### Event Listener

- For listening to events :

The object received can be String, Boolean, Long or JSONObject.

```java
    socket.on(eventname, new Emitter.Listener() {
                public void call(String eventName,Object object) {
                    
                    // Cast object to its proper datatype
                    System.out.println("Got message for :"+eventName+" data is :"+data);
                }
        }); 
```

- To send acknowledgement back to server

```java
    socket.on(eventname, new Emitter.AckListener() {
            public void call(String eventName,Object object, Ack ack) {
                
                // Cast object to its proper datatype                     
                System.out.println("Got message :: " + object);
                /...
                    Some logic goes here
                .../
                if (error){
                
                ack.call(eventName,error,null);
                
                }else{
                
                //Data can be of any data type
                
                ack.call(eventName,null,data);
                }
                
                //Both error and data can be sent to server
                
                ack.call(eventName,error,data);
            }
        });
        
```

Implementing Pub-Sub via channels
---------------------------------

#### Creating channel

- For creating and subscribing to channels:

```java
    Socket.Channel channel = socket.createChannel(channelName);
    //Socket.Channel channel = socket.createChannel("yolo"); 
    
    
    /**
     * without acknowledgement
     */
     channel.subscribe();
     
    /**
     * with acknowledgement
     */
     
    channel.subscribe(new Ack() {
                public void call(String channelName, Object error, Object data) {
                    if (error == null) {
                        System.out.println("Subscribed to channel "+channelName+" successfully");
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




#### Publishing event on channel

- For publishing event :

```java
       // message can have any data type
    /**
     * without acknowledgement
     */
     channel.publish(message);
     
    /**
     * with acknowledgement
     */
       channel.publish(message, new Ack() {
                public void call(String channelName,Object error, Object data) {
                    if (error == null) {
                        System.out.println("Published message to channel "+channelName+" successfully");
                    }
                }
        });
        
``` 
 
#### Listening to channel

- For listening to channel event :

```java
    channel.onMessage(new Emitter.Listener() {
             public void call(String channelName , Object object) {
                
                 System.out.println("Got message for channel "+channelName+" data is "+data);
                 
             }
         });
``` 
 
<!--###### Pub-sub without creating channel-->
#### Un-subscribing to channel

```java

    /**
     * without acknowledgement
     */
     
     channel.unsubscribe();
     
    /**
     * with acknowledgement
     */
     
    channel.unsubscribe(new Ack() {
                public void call(String channelName, Object error, Object data) {
                    if (error == null) {
                        System.out.println("channel unsubscribed successfully");
                    }
                }
        });    
```
 
#### Handling SSL connection with server
 
`WebSocketFactory` class is responsible for creating websocket instances and handling settings with server, for more
information visit [ here ](https://github.com/TakahikoKawasaki/nv-websocket-client/blob/master/README.md)

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
#### Star the repo. if you love the client :).
 
