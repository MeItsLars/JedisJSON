package nl.itslars.jedisjson.listeners;

import nl.itslars.jedisjson.listeners.types.JedisJSONPacketListener;
import nl.itslars.jedisjson.packet.JedisJSONPacketHeader;

import java.util.HashMap;
import java.util.Map;

public class NewIncomingPacketHandler {

    private Map<String, JedisJSONPacketListener<?>> incomingPacketListeners = new HashMap<>();

    public void registerNewIncomingPacketListener(String clazz, JedisJSONPacketListener<?> listener) {
        if (incomingPacketListeners.put(clazz, listener) != null) {
            throw new IllegalStateException("You may not register two listeners for a packet class! (" + clazz + ")");
        }
    }

    public void onReceive(JedisJSONPacketHeader header) {
        JedisJSONPacketListener<?> listener = incomingPacketListeners.get(header.getClazz());
        if (listener == null) return;
        JedisPacketHandler.receiveIncomingPacket(listener, header);
    }
}
