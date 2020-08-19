package nl.itslars.jedisjson.listeners;

import lombok.Getter;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.listeners.types.JedisJSONPacketListener;
import nl.itslars.jedisjson.packet.JedisJSONPacket;
import nl.itslars.jedisjson.packet.JedisJSONPacketHeader;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JedisPacketHandler extends JedisPubSub {

    private JedisJSON client;
    @Getter
    private NewIncomingPacketHandler newIncomingPacketHandler;
    private Map<String, JedisJSONPacketListener<?>> listeners = new ConcurrentHashMap<>();

    public JedisPacketHandler(JedisJSON client) {
        this.client = client;
        this.newIncomingPacketHandler = new NewIncomingPacketHandler();
    }

    public void registerListener(String id, JedisJSONPacketListener<?> listener, boolean isNewChannel) {
        listeners.put(id, listener);
        if (isNewChannel) subscribe(id);
    }

    public void unregisterListener(String id) {
        listeners.remove(id);
    }

    @Override
    public void onMessage(String channel, String packet) {
        JedisJSONPacketHeader header = client.getDefaultGson().fromJson(packet, JedisJSONPacketHeader.class);
        String packetID = header.getId();

        // If the packet ID is attached to a packet listener, we forward it to that listener
        // Otherwise, we need to forward the packet to the incoming packet handler
        JedisJSONPacketListener<?> listener = listeners.get(packetID);
        if (listener == null) {
            newIncomingPacketHandler.onReceive(header);
        } else {
            receiveIncomingPacket(listener, header);
        }
    }

    public static <T extends JedisJSONPacket> void receiveIncomingPacket(JedisJSONPacketListener<T> listener, JedisJSONPacketHeader header) {
        T t = listener.getPacketGson().fromJson(header.getData(), listener.getPacketType());
        t.applyHeader(header);
        listener.accept(t);
    }
}
