package nl.itslars.jedisjson.packet.conversation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

@AllArgsConstructor
@Getter
public class ConversationEndPacket extends JedisJSONPacket {

    private String conversationID;

}
