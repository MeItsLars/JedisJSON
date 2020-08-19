package nl.itslars.jedisjson.listeners.types;

import com.google.gson.Gson;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

import java.lang.reflect.Type;

public interface JedisJSONPacketListener<T extends JedisJSONPacket> {

    void accept(T t);

    Gson getPacketGson();

   Type getPacketType();

}
