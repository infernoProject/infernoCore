package ru.infernoproject.worldd.world.creature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.common.utils.ByteConvertible;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.WorldObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldCreature extends WorldObject {

    private String name;

    private int healthCurrent = 100;
    private int healthMax = 100;

    private boolean dead = false;

    private WorldNotificationListener notificationListener;
    private Map<Integer, Long> coolDownMap;

    private static final Logger logger = LoggerFactory.getLogger(WorldCreature.class);

    public WorldCreature(WorldNotificationListener notificationListener, String name) {
        this.name = name;
        this.notificationListener = notificationListener;
        this.coolDownMap = new ConcurrentHashMap<>();
    }

    private void onEvent(byte type, ByteConvertible data) {
        if (notificationListener != null) {
            notificationListener.onEvent(type, data);
        }
    }

    public void processDamage(WorldCreature caster, int damage) {
        if (!dead) {
            healthCurrent = Math.max(0, healthCurrent - damage);

            onEvent(WorldEventType.DAMAGE, new ByteArray().put(damage).put(healthCurrent).put(healthMax));
            logger.debug(String.format("%s damaged %s for %d hit points.", caster.getName(), name, damage));
        }

        if ((!dead)&&(healthCurrent == 0)) {
            dead = true;

            onEvent(WorldEventType.DEATH, new ByteArray().put(healthCurrent).put(healthMax));
            logger.debug(String.format("%s killed %s.", caster.getName(), name));
        }
    }

    public void processHeal(WorldCreature caster, int heal) {
        if (!dead &&(healthCurrent < healthMax)) {
            healthCurrent = Math.min(healthMax, healthCurrent + heal);

            onEvent(WorldEventType.HEAL, new ByteArray().put(heal).put(healthCurrent).put(healthMax));
            logger.debug(String.format("%s healed %s for %d hit points.", caster.getName(), name, heal));
        }
    }

    public void processRevive(WorldCreature caster) {
        if (dead) {
            dead = false;
            healthCurrent = healthMax;

            onEvent(WorldEventType.REVIVE, new ByteArray().put(healthCurrent).put(healthMax));
            logger.debug(String.format("%s revived %s.", caster.getName(), name));
        }
    }

    public String getName() {
        return name;
    }

    public boolean isDead() {
        return dead;
    }

    public void addCoolDown(int spellId, int coolDown) {
        coolDownMap.put(spellId, System.currentTimeMillis() + coolDown);
    }

    public boolean hasCoolDown(int spellId) {
        return coolDownMap.getOrDefault(spellId, System.currentTimeMillis()) > System.currentTimeMillis();
    }

    public void update(long diff) {
        coolDownMap.keySet().parallelStream()
            .filter(spellId -> coolDownMap.get(spellId) <= System.currentTimeMillis())
            .forEach(coolDownMap::remove);
    }
}
