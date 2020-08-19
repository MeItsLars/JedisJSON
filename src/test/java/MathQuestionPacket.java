import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.itslars.jedisjson.packet.JedisJSONPacket;

@AllArgsConstructor
@Getter
public class MathQuestionPacket extends JedisJSONPacket {

    private String question;
    private int response;

}
