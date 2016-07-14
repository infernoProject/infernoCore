package ru.linachan.inferno.common.codec.xor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class XOREncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] in, ByteBuf out) throws Exception {
        byte[] data = XORUtils.xencode(in);

        out.writeShort(data.length);
        out.writeBytes(data);
    }
}
