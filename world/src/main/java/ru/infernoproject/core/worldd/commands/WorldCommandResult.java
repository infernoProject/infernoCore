package ru.infernoproject.core.worldd.commands;

import ru.infernoproject.core.common.utils.ByteConvertible;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WorldCommandResult implements ByteConvertible {

    public enum State {
        SUCCESS, FAILURE
    }

    private final State state;
    private String[] output;

    public WorldCommandResult(State state) {
        this.state = state;
    }

    public void setOutput(String[] output) {
        this.output = output;
    }

    public static WorldCommandResult success(String... output) {
        WorldCommandResult result = new WorldCommandResult(State.SUCCESS);
        result.setOutput(output);
        return result;
    }

    public static WorldCommandResult failure(String... output) {
        WorldCommandResult result = new WorldCommandResult(State.FAILURE);
        result.setOutput(output);
        return result;
    }

    @Override
    public byte[] toByteArray() {
        byte stateByte;
        switch (state) {
            case SUCCESS:
                stateByte = 0x00;
                break;
            case FAILURE:
                stateByte = 0x01;
                break;
            default:
                stateByte = (byte) 0xFF;
                break;
        }

        Integer outputLength = 5;
        List<byte[]> outputBytes = new ArrayList<>();

        for (String outputLine: output) {
            byte[] outputLineBytes = outputLine.getBytes();
            outputBytes.add(outputLineBytes);
            outputLength += 4 + outputLineBytes.length;
        }

        ByteBuffer outputBuffer = ByteBuffer.allocate(outputLength);

        outputBuffer.put(stateByte);
        outputBuffer.putInt(outputBytes.size());

        for (byte[] outputLineBytes: outputBytes) {
            outputBuffer.putInt(outputLineBytes.length);
            outputBuffer.put(outputLineBytes);
        }

        return outputBuffer.array();
    }
}
