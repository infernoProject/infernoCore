package ru.infernoproject.worldd.world.chat;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.map.WorldCell;
import ru.infernoproject.worldd.map.WorldMap;
import ru.infernoproject.worldd.map.WorldMapManager;
import ru.infernoproject.worldd.world.player.WorldPlayer;

public class ChatManager {

    private final WorldMapManager worldMapManager;

    public ChatManager(WorldMapManager worldMapManager) {
        this.worldMapManager = worldMapManager;
    }

    public void sendLocalMessage(WorldPlayer sender, String message) {
        WorldMap map = worldMapManager.getMap(sender.getPosition());
        WorldCell cell = map.getCellByPosition(sender.getPosition());

        cell.onEvent(sender, WorldEventType.CHAT_MESSAGE, new ByteArray()
            .put(sender.getOID())
            .put(sender.getName())
            .put(message)
        );
    }

    public void sendBroadcastMessage(WorldPlayer sender, String message) {
        WorldMap map = worldMapManager.getMap(sender.getPosition());

        map.onEvent(sender, WorldEventType.CHAT_MESSAGE, new ByteArray()
            .put(sender.getOID())
            .put(sender.getName())
            .put(message)
        );
    }

    public void sendPrivateMessage(WorldPlayer sender, WorldPlayer target, String message) {
        WorldMap map = worldMapManager.getMap(target.getPosition());
        WorldCell cell = map.getCellByPosition(target.getPosition());

        ByteArray chatMessage = new ByteArray()
            .put(sender.getOID())
            .put(sender.getName())
            .put(message);

        target.onEvent(cell, WorldEventType.CHAT_MESSAGE, new ByteArray()
            .put(sender.getAttributes())
            .put(chatMessage)
        );
    }
}
