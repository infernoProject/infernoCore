package ru.infernoproject.worldd.script.impl;

import ru.infernoproject.common.auth.sql.Session;
import ru.infernoproject.common.db.DataSourceManager;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.worldd.script.ScriptableObject;

public interface CommandBase extends ScriptableObject {

    ByteArray execute(DataSourceManager dataSourceManager, Session session, String[] args);
}
