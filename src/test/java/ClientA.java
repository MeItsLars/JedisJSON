import lombok.SneakyThrows;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.listeners.types.Channel;

public class ClientA {

    @SneakyThrows
    public static void main(String[] args) {
        JedisJSON jedisJSON = new JedisJSON("localhost", 6379, "ClientA");

        Channel<MathQuestionPacket> channel = jedisJSON.enterChannel("math", MathQuestionPacket.class);
        channel.broadcast(new MathQuestionPacket("2+2", 0));
    }
}
