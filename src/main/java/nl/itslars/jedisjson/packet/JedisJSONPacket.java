package nl.itslars.jedisjson.packet;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class JedisJSONPacket {

    private transient String id;
    private transient String source;

    public void applyHeader(JedisJSONPacketHeader header) {
        this.id = header.getId();
        this.source = header.getSource();
    }
}
