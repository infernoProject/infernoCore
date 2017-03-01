package ru.infernoproject.worldd.world;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

public class WorldEvent implements ByteConvertible {

    private byte type;
    private int quantifier;

    private int healthCurrent;
    private int healthMax;

    public WorldEvent(byte type, int quantifier, int healthCurrent, int healthMax) {
        this.type = type;
        this.quantifier = quantifier;

        this.healthCurrent = healthCurrent;
        this.healthMax = healthMax;
    }

    public WorldEvent(byte type, int healthCurrent, int healthMax) {
        this.type = type;
        this.quantifier = 0;

        this.healthCurrent = healthCurrent;
        this.healthMax = healthMax;
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(type).put(quantifier)
            .put(healthCurrent).put(healthMax)
            .toByteArray();
    }
}