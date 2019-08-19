package ru.infernoproject.worldd.world.items;

import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.db.sql.utils.SQLFilter;
import ru.infernoproject.worldd.world.items.sql.Item;

import java.sql.SQLException;
import java.util.List;

public class ItemManager {

    private final DataSourceManager dataSourceManager;

    public ItemManager(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public List<Item> findItemsByName(String name) throws SQLException {
        return dataSourceManager.query(Item.class).select()
            .filter(new SQLFilter("name").like(name))
            .fetchAll();
    }

    public Item getItemById(int id) throws SQLException {
        return dataSourceManager.query(Item.class).select()
            .filter(new SQLFilter("id").eq(id))
            .fetchOne();
    }
}
