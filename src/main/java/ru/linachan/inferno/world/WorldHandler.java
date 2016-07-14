package ru.linachan.inferno.world;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.mmo.codec.Message;
import ru.linachan.yggdrasil.common.vector.Vector2;

import java.nio.ByteBuffer;
import java.util.List;

@ChannelHandler.Sharable
public class WorldHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(WorldHandler.class);
    private World world;

    public WorldHandler() {
        world = new World(new Vector2<>(800.0, 800.0), new Vector2<>(100, 100));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message request = (Message) msg;
        byte[] response = new byte[0];

        if (request.session() != null) {
            Player player = world.getPlayer(request.session());

            short opCode = request.data().getShort();
            switch (opCode) {
                case 0x01:
                    player.setPosition(new Vector2<>(
                        request.data().getDouble(), request.data().getDouble()
                    ));
                    world.updatePlayer(player);
                    break;
                case 0x02:
                    player.setInterestAreaSize(new Vector2<>(
                        request.data().getDouble(), request.data().getDouble()
                    ));
                    world.updatePlayer(player);
                    break;
                case 0x03:
                    List<byte[]> events = player.getInterestArea().getEvents();

                    final int[] eventsLength = { 0 };
                    events.stream().forEach(event -> eventsLength[0] += event.length);

                    ByteBuffer eventsData = ByteBuffer.allocate(eventsLength[0] + (4 * (events.size() + 1)));
                    events.stream().forEach(event -> {
                        eventsData.putInt(event.length);
                        eventsData.put(event);
                    });

                    response = eventsData.array();
                    break;
            }
        }

        ctx.write(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}
