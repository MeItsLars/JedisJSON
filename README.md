# JedisJSON
JedisJSON is an easy to use wrapper library for Java 8 and higher, that combines Jedis and Gson to create a user-friendly format for sending packets to other clients, over Redis.
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
### Single packet sending
To send a single packet from client A to client B, you need to do the following:
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

On client B, to receive such incoming packets, you need to do:
```java
@SneakyThrows
public static void main(String[] args) {
    // Create a new JedisJSON instance
    JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientB");
    
    // Receive incoming MathQuestionPackets
    jedisJson.onReceive(MathQuestionPacket.class, packet -> {
        System.out.println("Received a question from " + packet.getSource() + ": " + packet.getQuestion());
    });
    
    // After 30 seconds, close the instance
    Thread.sleep(30000);
    jedisJson.shutdown();
}
```

### Single packet sending, with a reply
To send a single packet from client A to client B, but expect a response from client B, you need to do the following:
```java
@SneakyThrows
public static void main(String[] args) {
    // Create a new JedisJSON instance
    JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientA");
    
    // Send the packet to ClientB, but with an incoming reply, and a timeout of 10 seconds
    jedisJson.sendWithReply("ClientB", new MathQuestionPacket("2+2", 0), responsePacket -> {
        System.out.println("Response: " + responsePacket.getResponse());
    }, 10000);
    
    // After 30 seconds, close the instance
    Thread.sleep(30000);
    jedisJson.shutdown();
}
```
The timeout is the time that the library will wait to receive a response to this packet. If you want the library to wait forever, set the timeout value to ``Integer.MAX_VALUE``.
If you don't enter a timeout, the library will use the default timeout of 5 seconds.

To receive such response packet on client B, the code is simlar to the no-reply example, but with a little change: You must return a packet in the lambda:
```java
@SneakyThrows
public static void main(String[] args) {
	// Create a new JedisJSON instance
	JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientB");
  
	// Listen for an incoming math question, and return the result (in this case, the hard-coded result '4')
	jedisJson.onReceive(MathQuestionPacket.class, packet -> {
		System.out.println("Received a question from " + packet.getSource() + ": " + packet.getQuestion());
		return new MathQuestionPacket("", 4);
	});
  
	// After 30 seconds, close the instance
	Thread.sleep(30000);
	jedisJson.shutdown();
}
```

### Conversation with another client
If you want to start a session of multiple packets and response packets with another client, you can use a conversation.
A conversation essentially behaves like a finite state automata, in that it has states, and it can send questions and receive responses in each state.

Let's create a quick example, where we want client A to send two math questions to client B: ``1+1 and 2+2``. Client B must then respond with the correct answer. If the answer is incorrect, client A must send the question again.

The finite state automata that belongs to this explanation is as follows:
![alt text](https://i.ibb.co/KNXwSTb/Jedis-JSONExample.png)
Client A has the upper automata, client B has the bottom one.
As you can see, client A has 3 states, and client B has 2. Let's put this to JedisJSON code:
#### Client A
```java
@SneakyThrows
public static void main(String[] args) {
	// Create a new JedisJSON instance
	JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientA");

	Conversation<MathQuestionPacket> conversation = jedisJson.createConversation(MathQuestionPacket.class, "ClientB", 0);
	// Create the initial send, (1+1)
	conversation.setState(0, new MathQuestionPacket("1+1", 0));
	// Listen for the response to the first question
	conversation.onState(0, response -> {
		// If the answer is correct, go to state 1 and ask '2+2', otherwise, stay in state 0 and ask '1+1'
		if (response.getResponse() == 2) {
			conversation.setState(1, new MathQuestionPacket("2+2", 0));
		} else {
			conversation.setState(0, new MathQuestionPacket("1+1", 0));
		}
	});
	// Listen for the response to the second question
	conversation.onState(1, response -> {
		// If the answer is correct, close the conversation, otherwise, stay in state 1 and ask '2+2'
		if (response.getResponse() == 4) {
			System.out.println("Terminated succesfully!");
			conversation.close();
			jedisJson.shutdown();
		} else {
			conversation.setState(1, new MathQuestionPacket("2+2", 0));
		}
	});
}
```
#### Client B
```java
@SneakyThrows
public static void main(String[] args) {
	// Create a new JedisJSON instance
	JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientB");

	// Accept incoming conversations
	jedisJson.acceptConversation(MathQuestionPacket.class, 0, conversation -> {
		conversation.onState(0, question -> {
			// Evaluate and answer the question
			String[] parts = question.getQuestion().split("\\+");
			int result = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
			conversation.setState(0, new MathQuestionPacket("", result));
		});
	});

	// After 30 seconds, close the instance
	Thread.sleep(30000);
	jedisJson.shutdown();
}
```
Test it for yourself! This will print 'Terminated succesfully!' on client A!
The Conversation code for the clients is also on GitHub.

### Channels
Lastly, I added support for registering to a channel. These are really easy to understand, as they are close to what PubSub actually is.
A code example for one client would be:
```java
@SneakyThrows
public static void main(String[] args) {
	// Create a new JedisJSON instance
	JedisJSON jedisJson = new JedisJSON("localhost", 6379, "ClientA");

	// Enter the channel
	Channel<MathQuestionPacket> channel = jedisJson.enterChannel("math", MathQuestionPacket.class);
	// Broadcasts a question on the channel
	channel.broadcast(new MathQuestionPacket("2+2", 0));
	// Receives incoming packets from other clients broadcasts
	channel.onReceive(result -> {
		System.out.println("Received a result: " + result.getResponse());
	});

	// After 30 seconds, close the instance
	Thread.sleep(30000);
	jedisJson.shutdown();
}
```

# Conclusion
I initially made this library for myself, but I dedicated quite some time to make it more than just a 'personal project'.
Feel free to use this library for free in all your projects! That's what I added the documentation for ;)
