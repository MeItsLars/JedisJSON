package nl.itslars.jedisjson.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class JedisJSONPacketHeader {

    private String id;
    private String source;
    private String data;
    private String clazz;

}
