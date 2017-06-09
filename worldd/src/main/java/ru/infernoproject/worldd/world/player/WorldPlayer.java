package ru.infernoproject.worldd.world.player;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.movement.WorldPosition;

public class WorldPlayer extends WorldCreature {

    private final CharacterInfo characterInfo;

    public WorldPlayer(WorldNotificationListener notificationListener, CharacterInfo characterInfo) {
        super(notificationListener, String.format(
            "%s %s", characterInfo.firstName, characterInfo.lastName
        ));

        this.characterInfo = characterInfo;

        /*
        setPosition(new WorldPosition(
            characterInfo.location,
            characterInfo.positionX,
            characterInfo.positionY,
            characterInfo.positionZ,
            characterInfo.orientation
        ));
        */
    }

    public CharacterInfo getCharacterInfo() {
        return characterInfo;
    }
}
