package nl.itslars.jedisjson.listeners.types;

import com.google.gson.Gson;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class Channel<T extends JedisJSONPacket> implements JedisJSONPacketListener<T> {

    private JedisJSON client;
    private Gson gson;
    private Type packetType;
    private String channelName;
    private Consumer<T> consumer;

    public Channel(JedisJSON client, Gson gson, Type packetType, String channelName) {
        this.client = client;
        this.gson = gson;
        this.packetType = packetType;
        this.channelName = channelName;
    }

    public void onReceive(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    public void broadcast(T t) {
        client.sendWithId(channelName, t, channelName, gson);
    }

    @Override
    public void accept(T t) {
        if (consumer != null && !t.getSource().equals(client.getClientName())) consumer.accept(t);
    }

    @Override
    public Gson getPacketGson() {
        return gson;
    }

    @Override
    public Type getPacketType() {
        return packetType;
    }
}
