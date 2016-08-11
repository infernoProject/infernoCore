package ru.linachan.inferno.world;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.inferno.chat.Chat;
import ru.linachan.inferno.common.codec.Message;
import ru.linachan.inferno.common.vector.Vector2;

import java.nio.ByteBuffer;
import java.util.List;

@ChannelHandler.Sharable
public class WorldHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(WorldHandler.class);

    private World world;
    private Chat worldChat;

    public WorldHandler() {
        world = new World(new Vector2<>(800.0, 800.0), new Vector2<>(100, 100));
        worldChat = new Chat();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message request = (Message) msg;
        ByteBuffer response = ByteBuffer.wrap(new byte[] { 0x00, 0x00 });

        if (request.session() != null) {
            Player player = world.getPlayer(request.session());

            short opCode = request.data().getShort();
            switch (opCode) {
                case 0x01: // Set Player position
                    player.setPosition(new Vector2<>(
                        request.data().getDouble(), request.data().getDouble()
                    ));
                    world.updatePlayer(player);

                    response = ByteBuffer.allocate(18);
                    response.putShort((short) 0x01);

                    response.putDouble(player.getPosition().getX());
                    response.putDouble(player.getPosition().getY());
                    break;
                case 0x02: // Set Player interest area
                    player.setInterestAreaSize(new Vector2<>(
                        request.data().getDouble(), request.data().getDouble()
                    ));
                    world.updatePlayer(player);

                    response = ByteBuffer.allocate(18);
                    response.putShort((short) 0x02);

                    response.putDouble(player.getPosition().getX());
                    response.putDouble(player.getPosition().getY());
                    break;
                case 0x03: // Get interest events
                    List<byte[]> events = player.getInterestArea().getEvents();

                    final int[] eventsLength = { 0 };
                    events.stream().forEach(event -> eventsLength[0] += event.length);

                    response = ByteBuffer.allocate(eventsLength[0] + (4 * (events.size() + 1)) + 2);
                    response.putShort((short) 0x03);
                    response.putInt(events.size());

                    ByteBuffer finalResponse = response;
                    events.stream().forEach(event -> {
                        finalResponse.putInt(event.length);
                        finalResponse.put(event);
                    });

                    break;
                case 0x04: // Send chat message
                    worldChat.sendMessage(player, request);
                    break;
                case 0x05:
                    response = ByteBuffer.allocate(50);
                    response.putShort((short) 0x05);

                    response.putLong(player.getID().getLeastSignificantBits());
                    response.putLong(player.getID().getMostSignificantBits());

                    response.putDouble(player.getPosition().getX());
                    response.putDouble(player.getPosition().getY());

                    response.putDouble(player.getInterestAreaSize().getX());
                    response.putDouble(player.getInterestAreaSize().getY());
                    break;
            }
        }

        ctx.write(response.array());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        logger.error("Shit happens: {}", cause);
        ctx.close();
    }

    public World getWorld() {
        return world;
    }
}
