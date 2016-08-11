package ru.linachan.inferno.common.codec.xor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class XORDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            if (in.readableBytes() < 2)
                break;

            short dataLength = in.readShort();
            if (dataLength >= 0) {
                if (in.readableBytes() < dataLength)
                    break;

                byte[] data = new byte[dataLength];
                in.readBytes(data);

                out.add(XORUtils.xdecode(data));
            }
        }
    }
}
