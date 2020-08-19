package nl.itslars.jedisjson.listeners.types;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.packet.JedisJSONPacket;
import nl.itslars.jedisjson.packet.conversation.ConversationEndPacket;
import nl.itslars.jedisjson.packet.conversation.ConversationStartPacket;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Conversation<T extends JedisJSONPacket> implements JedisJSONPacketListener<T>, Closeable {

    private JedisJSON client;
    private Gson gson;
    private Type packetType;

    private String target;
    private String conversationID;
    @Setter
    private int state;
    private Map<Integer, Consumer<T>> stateConsumers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Conversation(JedisJSON client, Gson gson, Type packetType, String target, String conversationID, boolean start, int initialState) {
        this.client = client;
        this.gson = gson;
        this.packetType = packetType;

        this.target = target;
        this.conversationID = conversationID;
        this.state = initialState;

        if (start) client.send(target, new ConversationStartPacket(((Class<T>) packetType).getSimpleName(), conversationID), gson);
    }

    public Conversation<T> onState(int state, Consumer<T> consumer) {
        stateConsumers.put(state, consumer);
        return this;
    }

    public void setState(int state, T t) {
        this.state = state;
        client.sendWithId(target, t, conversationID, gson);
    }

    @Override
    public void accept(T t) {
        Consumer<T> consumer = stateConsumers.get(state);
        if (consumer != null) consumer.accept(t);
    }

    @Override
    public Gson getPacketGson() {
        return gson;
    }

    @Override
    public Type getPacketType() {
        return packetType;
    }

    @Override
    public void close() {
        client.send(target, new ConversationEndPacket(conversationID));
        client.getJedisPacketHandler().unregisterListener(conversationID);
    }

    private static Map<Type, ConversationData<?>> acceptableConversations = new HashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void initializeConversationStructure(JedisJSON client) {
        client.onReceive(ConversationStartPacket.class, packet -> {
            acceptableConversations.forEach((type, data) -> {
                if (!type.getTypeName().equals(packet.getClazz())) return;

                Conversation conversation = new Conversation(client,
                        data.getGson(),
                        data.getType(),
                        packet.getSource(),
                        packet.getConversationID(),
                        false,
                        data.getInitialState());

                client.getJedisPacketHandler().registerListener(packet.getConversationID(), conversation, false);
                data.getConsumer().accept(conversation);
            });
        });

        client.onReceive(ConversationEndPacket.class, packet -> {
            client.getJedisPacketHandler().unregisterListener(packet.getConversationID());
        });
    }

    public static void acceptConversation(ConversationData<?> data) {
        acceptableConversations.put(data.getType(), data);
    }

    @AllArgsConstructor
    @Getter
    public static class ConversationData<T extends JedisJSONPacket> {

        private Type type;
        private Gson gson;
        private int initialState;
        private Consumer<Conversation<T>> consumer;

    }
}
