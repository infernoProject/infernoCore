package ru.infernoproject.worldd.world.player.inventory;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.world.player.inventory.sql.CharacterInventoryItem;

import java.sql.SQLException;
import java.util.List;

public class InventoryManager {

    private final DataSourceManager dataSourceManager;

    public InventoryManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<CharacterInventoryItem> getCharacterInventory(int characterId) throws SQLException {
        return dataSourceManager.query(CharacterInventoryItem.class).select()
            .filter(new SQLFilter("character_id").eq(characterId))
            .fetchAll();
    }
}
