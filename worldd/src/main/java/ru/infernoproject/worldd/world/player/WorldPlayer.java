package ru.infernoproject.worldd.world.player;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.worldd.world.MovementInfo;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.creature.WorldCreature;

public class WorldPlayer extends WorldCreature {

    private final CharacterInfo characterInfo;

    public WorldPlayer(WorldNotificationListener notificationListener, CharacterInfo characterInfo) {
        super(notificationListener, String.format(
            "%s %s", characterInfo.firstName, characterInfo.lastName
        ));

        this.characterInfo = characterInfo;
    }

    public void handleFall(MovementInfo move) {
        // TODO(aderyugin): Implement movement handling
    }

    public void handleMove(byte opCode, MovementInfo move) {
        // TODO(aderyugin): Implement movement handling
    }

    public CharacterInfo getCharacterInfo() {
        return characterInfo;
    }
}
