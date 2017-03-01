package ru.infernoproject.worldd.world.player;

import ru.infernoproject.worldd.characters.sql.CharacterInfo;
import ru.infernoproject.worldd.data.MovementInfo;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.creature.WorldCreature;

public class WorldPlayer extends WorldCreature {

    private final CharacterInfo characterInfo;

    public WorldPlayer(WorldNotificationListener notificationListener, CharacterInfo characterInfo) {
        super(notificationListener, String.format(
            "%s %s", characterInfo.getFirstName(), characterInfo.getLastName()
        ));

        this.characterInfo = characterInfo;
    }

    public void handleFall(MovementInfo move) {

    }

    public void handleMove(byte opCode, MovementInfo move) {

    }

    public CharacterInfo getCharacterInfo() {
        return characterInfo;
    }
}
