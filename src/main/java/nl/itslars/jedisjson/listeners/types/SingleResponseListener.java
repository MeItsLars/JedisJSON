package nl.itslars.jedisjson.listeners.types;

import com.google.gson.Gson;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

import java.lang.reflect.Type;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SingleResponseListener<T extends JedisJSONPacket> implements JedisJSONPacketListener<T> {

    public static final int DEFAULT_SINGLE_RESPONSE_TIMEOUT = 5000;

    private JedisJSON client;
    private Gson gson;
    private Type packetType;
    private Consumer<T> replyConsumer;
    private Timer timer;

    public SingleResponseListener(JedisJSON client, Gson gson, Type packetType, String packetID, Consumer<T> replyConsumer, int timeout) {
        this.client = client;
        this.gson = gson;
        this.packetType = packetType;
        this.replyConsumer = replyConsumer;

        // We unregister this listener after #timeout milliseconds
        // This is required, otherwise a memory leak would exist if replies never came in
        // If the user doesn't care about such a memory leak, they can set the timeout value to Integer.MAX_VALUE, this will prevent the timer from starting
        if (timeout == Integer.MAX_VALUE) return;
        this.timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                client.getJedisPacketHandler().unregisterListener(packetID);
                timer.cancel();
            }
        }, timeout);
    }

    @Override
    public void accept(T t) {
        replyConsumer.accept(t);
        client.getJedisPacketHandler().unregisterListener(t.getId());
        timer.cancel();
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
