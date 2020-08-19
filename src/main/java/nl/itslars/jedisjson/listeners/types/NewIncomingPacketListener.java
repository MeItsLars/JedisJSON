package nl.itslars.jedisjson.listeners.types;

import com.google.gson.Gson;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

import java.lang.reflect.Type;
import java.util.function.Consumer;

public class NewIncomingPacketListener<T extends JedisJSONPacket> implements JedisJSONPacketListener<T> {

    private Gson gson;
    private Type packetType;
    private Consumer<T> replyConsumer;

    public NewIncomingPacketListener(Gson gson, Type packetType, Consumer<T> replyConsumer) {
        this.gson = gson;
        this.packetType = packetType;
        this.replyConsumer = replyConsumer;
    }

    @Override
    public void accept(T t) {
        replyConsumer.accept(t);
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
