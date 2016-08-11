package ru.linachan.inferno.world.interest;

import ru.linachan.inferno.world.Player;
import ru.linachan.inferno.world.Region;
import ru.linachan.inferno.world.World;

public class InterestManager {

    private World world;

    public InterestManager(World worldProcessor) {
        world = worldProcessor;
    }

    public void onUpdate(InterestObject interestObject, Region region) {
        for (Player worldPlayer: world.getPlayerData()) {
            if (!worldPlayer.equals(interestObject)) {
                boolean interested = region == null || worldPlayer.getInterestArea().isInterestedIn(region.getPosition());
                boolean subscribed = worldPlayer.getInterestArea().isSubscribed(interestObject);

                InterestEvent event = null;

                if (interested && subscribed) {
                    event = new InterestEvent(InterestEventType.UPDATE, interestObject);
                } else if (!interested && subscribed) {
                    event = new InterestEvent(InterestEventType.LEAVE, interestObject);
                } else if (interested) {
                    event = new InterestEvent(InterestEventType.ENTER, interestObject);
                }

                if (event != null) {
                    worldPlayer.getInterestArea().onEvent(event);
                }
            }
        }
    }
}
