package ru.infernoproject.worldd.world.player.inventory.sql;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.common.db.sql.annotations.SQLField;
import ru.infernoproject.common.db.sql.annotations.SQLObject;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.world.items.sql.Item;

@SQLObject(database = "characters", table = "character_inventory")
public class CharacterInventoryItem implements SQLObjectWrapper, ByteConvertible {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "character_id")
    public CharacterInfo character;

    @SQLField(column = "item_id")
    public Item item;

    @SQLField(column = "quantity")
    public int quantity;

    @SQLField(column = "durability")
    public int durability;

    @SQLField(column = "inventory_type")
    public int inventoryType;

    @SQLField(column = "inventory_id")
    public int inventoryId;

    @Override
    public byte[] toByteArray() {
        return new ByteArray()
            .put(inventoryType).put(inventoryId)
            .put(quantity).put(durability)
            .put(item)
            .toByteArray();
    }
}
