package ru.infernoproject.worldd.world.items.sql;

import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;

import java.util.Arrays;

@SQLObject(database = "objects", table = "items")
public class Item implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "name")
    public String name;

    @SQLField(column = "sell_price")
    public int sellPrice;
    @SQLField(column = "vendor_price")
    public int vendorPrice;

    @SQLField(column = "max_stack")
    public int maxStack;
    @SQLField(column = "max_owned")
    public int maxOwned;

    @SQLField(column = "durability")
    public int durability;

    @SQLField(column = "allowed_slots")
    public String allowedSlots;

    public boolean isEligibleForSlot(int slot) {
        return Arrays.stream(allowedSlots.split(","))
            .filter(slotId -> !slotId.isEmpty())
            .map(Integer::parseInt)
            .anyMatch(slotId -> slotId == slot);
    }

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(id).put(name)
            .put(sellPrice).put(vendorPrice)
            .put(maxStack).put(maxOwned)
            .put(allowedSlots)
            .toByteArray();
    }
}
