package nl.itslars.jedisjson;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.SneakyThrows;
import nl.itslars.jedisjson.listeners.JedisPacketHandler;
import nl.itslars.jedisjson.listeners.types.Channel;
import nl.itslars.jedisjson.listeners.types.Conversation;
import nl.itslars.jedisjson.listeners.types.NewIncomingPacketListener;
import nl.itslars.jedisjson.listeners.types.SingleResponseListener;
import nl.itslars.jedisjson.packet.JedisJSONPacket;
import nl.itslars.jedisjson.packet.JedisJSONPacketHeader;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

public class JedisJSON {

    //TODO: Logging
    private static final Logger LOGGER = Logger.getLogger(JedisJSON.class.getName());

    @Getter
    private String clientName;
    @Getter
    private Gson defaultGson = new Gson();

    // Jedis
    private JedisPool jedisPool;
    private Jedis publishJedis;
    private Jedis subscriptionJedis;
    @Getter
    private JedisPacketHandler jedisPacketHandler;

    public JedisJSON(String host, int port) throws UnknownHostException {
        this(host, port, InetAddress.getLocalHost().getHostName());
    }

    @SneakyThrows
    public JedisJSON(String host, int port, String clientName) {
        LOGGER.info("Enabling JedisJSON...");
        this.clientName = clientName;

        this.jedisPool = new JedisPool(new JedisPoolConfig(), host, port, 5000);
        this.publishJedis = jedisPool.getResource();
        this.subscriptionJedis = jedisPool.getResource();

        CompletableFuture<Void> future = new CompletableFuture<>();
        new Thread(() -> {
            jedisPacketHandler = new JedisPacketHandler(this);
            future.complete(null);
            subscriptionJedis.subscribe(jedisPacketHandler, clientName);
        }).start();
        future.get();

        Conversation.initializeConversationStructure(this);
        LOGGER.info("JedisJSON enabled!");
    }

    public void shutdown() {
        publishJedis.close();
        subscriptionJedis.close();
        jedisPool.close();
    }

    /**
     * This method sends the input packet to the given target in the following way:
     * 1) The packet is serialized
     * 2) The data is put into a JedisJSON packet header
     * 3) The header is serialized
     * 4) The header is sent to the given 'target' destination via Jedis
     * @param target The packet destination
     * @param packet The packet
     * @param packetID The unique ID of the packet
     * @param gson The GSON instance that should be used to serialize the packet
     * @param <T> The packet type
     */
    public <T extends JedisJSONPacket> void sendWithId(String target, T packet, String packetID, Gson gson) {
        packet.setId(packetID);
        packet.setSource(clientName);
        String data = gson.toJson(packet);
        JedisJSONPacketHeader header = new JedisJSONPacketHeader(packetID, clientName, data, packet.getClass().getSimpleName());
        String packetString = defaultGson.toJson(header);
        publishJedis.publish(target, packetString);
    }

    public <T extends JedisJSONPacket> void send(String target, T packet) {
        send(target, packet, defaultGson);
    }

    public <T extends JedisJSONPacket> void send(String target, T packet, Gson gson) {
        sendWithId(target, packet, UUID.randomUUID().toString(), gson);
    }

    public <T extends JedisJSONPacket> void sendWithReply(String target, T packet, Consumer<T> replyConsumer) {
        sendWithReply(target, packet, replyConsumer, SingleResponseListener.DEFAULT_SINGLE_RESPONSE_TIMEOUT, defaultGson);
    }

    public <T extends JedisJSONPacket> void sendWithReply(String target, T packet, Consumer<T> replyConsumer, Gson gson) {
        sendWithReply(target, packet, replyConsumer, SingleResponseListener.DEFAULT_SINGLE_RESPONSE_TIMEOUT, gson);
    }

    public <T extends JedisJSONPacket> void sendWithReply(String target, T packet, Consumer<T> replyConsumer, int timeout) {
        sendWithReply(target, packet, replyConsumer, timeout, defaultGson);
    }

    public <T extends JedisJSONPacket> void sendWithReply(String target, T packet, Consumer<T> replyConsumer, int timeout, Gson gson) {
        String packetID = UUID.randomUUID().toString();
        sendWithId(target, packet, packetID, gson);
        SingleResponseListener<T> listener = new SingleResponseListener<>(this, gson, packet.getClass(), packetID, replyConsumer, timeout);
        jedisPacketHandler.registerListener(packetID, listener, false);
    }

    public <T extends JedisJSONPacket> void onReceive(Class<T> clazz, Consumer<T> consumer) {
        onReceive(clazz, consumer, defaultGson);
    }

    public <T extends JedisJSONPacket> void onReceive(Class<T> clazz, Consumer<T> consumer, Gson gson) {
        NewIncomingPacketListener<T> listener = new NewIncomingPacketListener<>(gson, clazz, consumer);
        jedisPacketHandler.getNewIncomingPacketHandler().registerNewIncomingPacketListener(clazz.getSimpleName(), listener);
    }

    public <T extends JedisJSONPacket> void onReceive(Class<T> clazz, Function<T, T> function) {
        onReceive(clazz, function, defaultGson);
    }

    public <T extends JedisJSONPacket> void onReceive(Class<T> clazz, Function<T, T> function, Gson gson) {
        NewIncomingPacketListener<T> listener = new NewIncomingPacketListener<>(gson, clazz, packet -> {
            T response = function.apply(packet);
            sendWithId(packet.getSource(), response, packet.getId(), gson);
        });
        jedisPacketHandler.getNewIncomingPacketHandler().registerNewIncomingPacketListener(clazz.getSimpleName(), listener);
    }

    public <T extends JedisJSONPacket> Conversation<T> createConversation(Class<T> clazz, String target) {
        return createConversation(clazz, target, Integer.MAX_VALUE, defaultGson);
    }

    public <T extends JedisJSONPacket> Conversation<T> createConversation(Class<T> clazz, String target, int initialState) {
        return createConversation(clazz, target, initialState, defaultGson);
    }

    public <T extends JedisJSONPacket> Conversation<T> createConversation(Class<T> clazz, String target, Gson gson) {
        return createConversation(clazz, target, Integer.MAX_VALUE, gson);
    }

    public <T extends JedisJSONPacket> Conversation<T> createConversation(Class<T> clazz, String target, int initialState, Gson gson) {
        String conversationID = UUID.randomUUID().toString();
        Conversation<T> conversation = new Conversation<>(this, gson, clazz, target, conversationID, true, initialState);
        jedisPacketHandler.registerListener(conversationID, conversation, false);
        return conversation;
    }

    public <T extends JedisJSONPacket> void acceptConversation(Class<T> clazz, Consumer<Conversation<T>> consumer) {
        acceptConversation(clazz, defaultGson, Integer.MAX_VALUE, consumer);
    }

    public <T extends JedisJSONPacket> void acceptConversation(Class<T> clazz, int initialState, Consumer<Conversation<T>> consumer) {
        acceptConversation(clazz, defaultGson, initialState, consumer);
    }

    public <T extends JedisJSONPacket> void acceptConversation(Class<T> clazz, Gson gson, Consumer<Conversation<T>> consumer) {
        acceptConversation(clazz, gson, Integer.MAX_VALUE, consumer);
    }

    public <T extends JedisJSONPacket> void acceptConversation(Class<T> clazz, Gson gson, int initialState, Consumer<Conversation<T>> consumer) {
        Conversation.acceptConversation(new Conversation.ConversationData<>(clazz, gson, initialState, consumer));
    }

    public <T extends JedisJSONPacket> Channel<T> enterChannel(String name, Class<T> clazz) {
        return enterChannel(name, clazz, defaultGson);
    }

    public <T extends JedisJSONPacket> Channel<T> enterChannel(String name, Class<T> clazz, Gson gson) {
        Channel<T> channel = new Channel<T>(this, gson, clazz, name);
        jedisPacketHandler.registerListener(name, channel, true);
        return channel;
    }
}