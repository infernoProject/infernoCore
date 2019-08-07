package ru.infernoproject.common.xor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;

import java.nio.ByteBuffer;
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
            byte protocolVersion = in.getByte(in.readerIndex());
            if (protocolVersion != PROTOCOL_VERSION) {
                throw new IllegalStateException("Protocol version mismatch");
            }

            int dataLength = in.getInt(in.readerIndex() + 1);

            if (dataLength > in.readableBytes() - 5) {
                break;
            }

            in.skipBytes(5);
            if (dataLength >= 0) {
                ByteBuffer dataBuffer = ByteBuffer.allocate(dataLength);

                int totalBytesRead = 0;
                while (totalBytesRead < dataLength) {
                    int remainingBytes = dataLength - totalBytesRead;
                    byte[] readBuffer = new byte[Math.min(remainingBytes, in.readableBytes())];

                    in.readBytes(readBuffer);

                    dataBuffer.put(readBuffer);
                    totalBytesRead += readBuffer.length;
                }

                out.add(new ByteWrapper(XORUtils.xdecode(dataBuffer.array())));
            }
        }
    }
}
