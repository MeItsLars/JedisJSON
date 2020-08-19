import lombok.SneakyThrows;
import nl.itslars.jedisjson.JedisJSON;
import nl.itslars.jedisjson.listeners.types.Channel;

public class ClientB {

    @SneakyThrows
    public static void main(String[] args) {
        JedisJSON jedisJSON = new JedisJSON("localhost", 6379, "ClientB");

        Channel<MathQuestionPacket> channel = jedisJSON.enterChannel("math", MathQuestionPacket.class);
        channel.onReceive(packet -> System.out.println("Received " + packet.getQuestion() + ", " + packet.getResponse()));
    }
}