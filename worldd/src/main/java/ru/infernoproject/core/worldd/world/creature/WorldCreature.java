package ru.infernoproject.core.worldd.world.creature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.infernoproject.core.common.constants.WorldEventType;
import ru.infernoproject.core.worldd.scripts.base.Aura;
import ru.infernoproject.core.worldd.world.WorldNotificationListener;
import ru.infernoproject.core.worldd.world.WorldObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorldCreature extends WorldObject {

    private String name;

    private int healthCurrent = 100;
    private int healthMax = 100;

    private boolean dead = false;
    private List<Aura> auras;

    private WorldNotificationListener notificationListener;
    private Map<Integer, Long> coolDownMap;

    private static final Logger logger = LoggerFactory.getLogger(WorldCreature.class);

    public WorldCreature(WorldNotificationListener notificationListener, String name) {
        this.name = name;
        this.auras = new CopyOnWriteArrayList<>();
        this.notificationListener = notificationListener;
        this.coolDownMap = new ConcurrentHashMap<>();
    }

    private void onEvent(byte type, int quantifier, int duration, int healthCurrent, int healthMax) {
        if (notificationListener != null) {
            notificationListener.onEvent(type, quantifier, duration, healthCurrent, healthMax);
        }
    }

    public void processDamage(WorldCreature caster, int damage) {
        if (!dead) {
            healthCurrent = Math.max(0, healthCurrent - damage);

            onEvent(WorldEventType.DAMAGE, damage, 0, healthCurrent, healthMax);
            logger.debug(String.format("%s damaged %s for %d hit points.", caster.getName(), name, damage));
        }

        if ((!dead)&&(healthCurrent == 0)) {
            dead = true;

            onEvent(WorldEventType.DEATH, 0, 0, healthCurrent, healthMax);
            logger.debug(String.format("%s killed %s.", caster.getName(), name));
        }
    }

    public void processHeal(WorldCreature caster, int heal) {
        if (!dead &&(healthCurrent < healthMax)) {
            healthCurrent = Math.min(healthMax, healthCurrent + heal);

            onEvent(WorldEventType.HEAL, heal, 0, healthCurrent, healthMax);
            logger.debug(String.format("%s healed %s for %d hit points.", caster.getName(), name, heal));
        }
    }

    public void processRevive(WorldCreature caster) {
        if (dead) {
            dead = false;
            healthCurrent = healthMax;

            onEvent(WorldEventType.REVIVE, 0, 0, healthCurrent, healthMax);
            logger.debug(String.format("%s revived %s.", caster.getName(), name));
        }
    }

    public void processAura(WorldCreature caster, Aura aura) {
        if (!dead) {
            aura.setCaster(caster);

            Optional<Aura> auraOptional = auras.stream()
                .filter(auraInstance -> auraInstance.getId() == aura.getId())
                .findFirst();

            Aura auraInstance;

            if (auraOptional.isPresent()) {
                auraInstance = auraOptional.get();
                auraInstance.extendDuration(aura.getDuration());
            } else {
                auras.add(aura);
                auraInstance = aura;
            }

            onEvent(WorldEventType.AURA, auraInstance.getId(), auraInstance.getDuration(), healthCurrent, healthMax);
            logger.debug(String.format("%s buffed %s with %s.", caster.getName(), name, aura.getName()));
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
        auras.parallelStream().forEach(aura -> aura.process(diff, this));

        auras.parallelStream()
            .filter(aura -> aura.getDuration() <= 0)
            .forEach(aura -> {
                auras.remove(aura);
                onEvent(WorldEventType.UN_AURA, aura.getId(), 0, healthCurrent, healthMax);
            });

        coolDownMap.keySet().parallelStream()
            .filter(spellId -> coolDownMap.get(spellId) <= System.currentTimeMillis())
            .forEach(coolDownMap::remove);
    }
}
