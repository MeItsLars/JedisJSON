# JedisJSON
JedisJSON is an easy to use wrapper library, that combines Jedis and Gson to create a user-friendly format for sending packets to other clients, over Redis.
The principle is based on the PubSub of Redis.

You can use the JedisJSON library to send packets to one or more other clients. There are possibilities for:
- Sending a packet to another client
- Sending a packet to another client, and retrieving a response
- Starting a conversation with another client
- Entering a channel, over which packets can be broadcasted to all connected clients

# Packet Format
Each packet you send via JedisJSON needs to be serializable by GSON. The library uses a default GSON instance, but you can also supply your own, if your packets need more complex serialization.

After this packet is serialized, it is encapsulated in a JedisJSON packet header like this:
```json
{
  "id": "[Packet ID]",
  "source": "[Packet client source]",
  "clazz": "[Class name of the packet that is sent]",
  "data": "[Serialized packet]"
}
```

The ``id`` is used to determine the current conversation session, or channel.
The ``clazz`` is required for (among others) conversations.

# Usage
## Basics
To use this library, you can build it with maven, and add the dependency to your project.
All usage operates via the ``JedisJSON`` class. You need to create a new instance like this:
```java
JedisJSON jedisJson = new JedisJSON("host", port);
// Or:
JedisJSON jedisJson = new JedisJSON("host", port, "clientName");
```
The client name is the name of the client that registers itself to the system.
Client names **MUST** be unique, otherwise client to client message sending will not work properly.
If you don't supply a client name, the system will use the local host name (e.g. ``InetAddress.getLocalHost().getHostName()``).

Although Redis keeps working if you don't do this, you should close the JedisJSON instance after you are done:
```java
jedisJson.shutdown();
```

You need to have a packet class that can be sent. In this example, let's use the following packet (Lombok is used):
```java
@AllArgsConstructor
@Getter
public class MathQuestionPacket extends JedisJSONPacket {

    private String question;
    private int response;

}
```
Let's assume we have two clients: 'ClientA' and 'ClientB'. ClientA sends math requests to ClientB, and ClientB responds with the answer.
This packet is what I will be using in the following examples.
Any packet must extend the ``JedisJSONPacket`` class. This also makes sure that for all packets, you can retrieve the ID and source.

## Message sending possibilities
### Direct sending
To send a direct question from client A to client B, you need to do the following:
```java
@SneakyThrows
public static void main(String[] args) {
    // Create a new JedisJSON instance
    JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientA");
    // Send the question to client B
    jedisJson.send("ClientB", new MathQuestionPacket("2+2", 0));
    // Shut down the instance after the question is sent
    jedisJson.shutdown();
}
```

On client B, to receive such incoming questions, you need to do:
```java
@SneakyThrows
public static void main(String[] args) {
    // Create a new JedisJSON instance
    JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientB");
    // Receive incoming MathQuestionPackets
    jedisJson.onReceive(MathQuestionPacket.class, packet -> {
        System.out.println("Received a question from " + packet.getSource() + ": " + packet.getQuestion());
    });
    // After 10 seconds, close the instance
    Thread.sleep(10000);
    jedisJson.shutdown();
}
```




