package ru.infernoproject.worldd.world.player;

import ru.infernoproject.common.characters.sql.CharacterInfo;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteWrapper;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.creature.WorldCreature;
import ru.infernoproject.worldd.world.movement.WorldPosition;
import ru.infernoproject.worldd.world.object.WorldObjectType;

public class WorldPlayer extends WorldCreature {

    private final CharacterInfo characterInfo;

    public WorldPlayer(WorldNotificationListener notificationListener, CharacterInfo characterInfo) {
        super(notificationListener, String.format(
            "%s %s", characterInfo.firstName, characterInfo.lastName
        ));

        setType(WorldObjectType.PLAYER);

        this.characterInfo = characterInfo;

        setPosition(new WorldPosition(
            characterInfo.location,
            characterInfo.positionX,
            characterInfo.positionY,
            characterInfo.positionZ,
            characterInfo.orientation
        ));

        setLevel(characterInfo.level);
    }

    public CharacterInfo getCharacterInfo() {
        return characterInfo;
    }

    @Override
    public ByteArray getAttributes() {
        return super.getAttributes()
            .put(characterInfo.body);
    }

    public ByteArray getState() {
        return new ByteArray()
            .put(getMaxHitPoints())
            .put(getCurrentHitPoints())
            .put(getStatus());
    }
}
