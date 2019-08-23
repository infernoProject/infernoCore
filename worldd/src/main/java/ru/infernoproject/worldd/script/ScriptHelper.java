package ru.infernoproject.worldd.script;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.worldd.map.WorldMapManager;
import ru.infernoproject.worldd.world.items.ItemManager;
import ru.infernoproject.worldd.world.player.inventory.InventoryManager;

public class ScriptHelper {

    private final DataSourceManager dataSourceManager;
    private final WorldMapManager worldMapManager;
    private final ScriptManager scriptManager;
    private final ItemManager itemManager;
    private final InventoryManager inventoryManager;

    public ScriptHelper(DataSourceManager dataSourceManager, WorldMapManager worldMapManager, ScriptManager scriptManager, ItemManager itemManager, InventoryManager inventoryManager) {
        this.dataSourceManager = dataSourceManager;
        this.worldMapManager = worldMapManager;
        this.scriptManager = scriptManager;
        this.itemManager = itemManager;
        this.inventoryManager = inventoryManager;
    }

    public WorldMapManager getWorldMapManager() {
        return worldMapManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }
}
