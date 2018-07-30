package ru.infernoproject.common.xor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;

import java.util.List;

public class XORCodec extends ByteToMessageCodec<ByteArray> {

    private static final byte PROTOCOL_VERSION = 0x01;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteArray in, ByteBuf out) throws Exception {
        byte[] data = XORUtils.xencode(in.toByteArray());

        out.writeByte(PROTOCOL_VERSION);
        out.writeInt(data.length);
        out.writeBytes(data);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= 4) {
            byte protocolVersion = in.readByte();
            if (protocolVersion != PROTOCOL_VERSION) {
                throw new IllegalStateException("Protocol version mismatch");
            }

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
