package ru.infernoproject.common.xor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import ru.infernoproject.common.utils.ByteWrapper;

import java.util.List;

public class XORDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 4) {
            int dataLength = in.readInt();

            if ((dataLength >= 0)&&(in.readableBytes() >= dataLength)) {
                byte[] data = new byte[dataLength];
                in.readBytes(data);

                out.add(new ByteWrapper(XORUtils.xdecode(data)));
            } else {
                break;
            }
        }
    }
}
