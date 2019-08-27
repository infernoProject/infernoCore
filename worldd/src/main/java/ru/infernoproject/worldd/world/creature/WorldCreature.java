package ru.infernoproject.worldd.world.creature;

import ru.infernoproject.common.utils.ByteArray;
import ru.infernoproject.worldd.constants.WorldEventType;
import ru.infernoproject.worldd.script.impl.DamageOverTimeBase;
import ru.infernoproject.worldd.script.impl.EffectBase;
import ru.infernoproject.worldd.script.sql.EffectAttribute;
import ru.infernoproject.worldd.script.sql.EffectDirection;
import ru.infernoproject.worldd.script.sql.EffectType;
import ru.infernoproject.worldd.script.wrapper.DamageOverTimeWrapper;
import ru.infernoproject.worldd.script.wrapper.EffectWrapper;
import ru.infernoproject.worldd.world.WorldNotificationListener;
import ru.infernoproject.worldd.world.object.WorldObject;
import ru.infernoproject.worldd.world.object.WorldObjectType;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class WorldCreature extends WorldObject {

    private volatile int level = 1;
    private volatile long currentHitPoints = 10L;
    private volatile long maxHitPoints = 10L;
    private WorldCreatureStatus status = WorldCreatureStatus.ALIVE;

    private List<EffectWrapper> effects;
    private List<DamageOverTimeWrapper> damageOverTime;

    public WorldCreature(WorldNotificationListener notificationListener, String name) {
        super(notificationListener, name);

        setType(WorldObjectType.CREATURE);

        this.effects = new CopyOnWriteArrayList<>();
        this.damageOverTime = new CopyOnWriteArrayList<>();
    }

    @Override
    public ByteArray getAttributes() {
        return super.getAttributes()
            .put(level)
            .put(currentHitPoints)
            .put(maxHitPoints)
            .put(status.toString().toLowerCase());
    }

    public long getCurrentHitPoints() {
        return currentHitPoints;
    }

    protected void setCurrentHitPoints(long currentHitPoints) {
        this.currentHitPoints = currentHitPoints;
    }

    public long getMaxHitPoints() {
        return maxHitPoints;
    }

    protected void setMaxHitPoints(long maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public WorldCreatureStatus getStatus() {
        return status;
    }

    protected void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public synchronized void processHitPointChange(long hitPointChange) {
        hitPointChange = processEffects(EffectDirection.DEFENSE, EffectAttribute.POTENTIAL, hitPointChange);

        currentHitPoints = Math.min(Math.max(currentHitPoints + hitPointChange, 0), maxHitPoints);
        currentCell.onEvent(this, WorldEventType.HP_CHANGE, new ByteArray().put(hitPointChange));

        if (currentHitPoints == 0)
            processStatusChange(WorldCreatureStatus.DEAD);
    }

    public void processStatusChange(WorldCreatureStatus newStatus) {
        if (status != newStatus) {
            status = newStatus;

            currentCell.onEvent(this, WorldEventType.STATUS_CHANGE, new ByteArray().put(newStatus));
        }
    }

    public void applyEffect(EffectBase effect, WorldObject caster, long duration, EffectType type, EffectDirection direction, long id) {
        duration = processEffects(EffectDirection.DEFENSE, EffectAttribute.DURATION, duration);

        switch (type) {
            case AURA:
                applyAura(effect, caster, duration, type, direction, id);
                break;
            case BUFF:
            case DEBUFF:
                applyBuff(effect, caster, duration, type, direction, id);
                break;
        }
    }

    private void applyAura(EffectBase effect, WorldObject caster, long duration, EffectType type, EffectDirection direction, long id) {
        EffectWrapper wrapper = this.effects.parallelStream()
            .filter(effectWrapper -> effectWrapper.getId() == id && effectWrapper.getCaster().equals(caster))
            .findFirst().orElse(null);

        if (Objects.nonNull(wrapper)) {
            this.effects.remove(wrapper);
            currentCell.onEvent(this, WorldEventType.EFFECT_REMOVE, new ByteArray().put(id).put(caster.getOID()).put(type));
        } else {
            this.effects.add(new EffectWrapper(effect, caster, duration, type, direction, id));
            currentCell.onEvent(this, WorldEventType.EFFECT_ADD, new ByteArray().put(id).put(caster.getOID()).put(type).put(duration));
        }
    }

    private void applyBuff(EffectBase effect, WorldObject caster, long duration, EffectType type, EffectDirection direction, long id) {
        EffectWrapper wrapper = this.effects.parallelStream()
            .filter(effectWrapper -> effectWrapper.getId() == id && effectWrapper.getCaster().equals(caster))
            .findFirst().orElse(null);

        if (Objects.nonNull(wrapper)) {
            wrapper.extendDuration(duration);
            currentCell.onEvent(this, WorldEventType.EFFECT_UPDATE, new ByteArray().put(id).put(caster.getOID()).put(type).put(wrapper.getDuration()));
        } else {
            this.effects.add(new EffectWrapper(effect, caster, duration, type, direction, id));
            currentCell.onEvent(this, WorldEventType.EFFECT_ADD, new ByteArray().put(id).put(caster.getOID()).put(type).put(duration));
        }
    }

    public void applyDamageOverTime(DamageOverTimeBase damageOverTime, WorldObject caster, long duration, long tickInterval, long basicPotential, long id) {
        DamageOverTimeWrapper wrapper = this.damageOverTime.parallelStream()
            .filter(damageOverTimeWrapper -> damageOverTimeWrapper.getId() == id && damageOverTimeWrapper.getCaster().equals(caster))
            .findFirst().orElse(null);

        duration = processEffects(EffectDirection.DEFENSE, EffectAttribute.DURATION, duration);
        tickInterval = processEffects(EffectDirection.DEFENSE, EffectAttribute.TICK_TIME, tickInterval);

        if (Objects.nonNull(wrapper)) {
            wrapper.extendDuration(duration);
            currentCell.onEvent(this, WorldEventType.DOT_UPDATE, new ByteArray().put(id).put(caster.getOID()).put(wrapper.getDuration()));
        } else {
            this.damageOverTime.add(new DamageOverTimeWrapper(damageOverTime, caster, duration, tickInterval, basicPotential, id));
            currentCell.onEvent(this, WorldEventType.DOT_ADD, new ByteArray().put(id).put(caster.getOID()).put(duration));
        }
    }

    @Override
    public void update(long diff) {
        super.update(diff);

        this.effects.parallelStream()
            .peek(effect -> effect.process(diff, this))
            .filter(effect -> effect.getDuration() <= 0)
            .forEach(effect -> {
                effects.remove(effect);
                currentCell.onEvent(this, WorldEventType.EFFECT_REMOVE, new ByteArray().put(effect.getId()).put(effect.getCaster().getOID()).put(effect.getType()));
            });

        this.damageOverTime.parallelStream()
            .peek(dot -> dot.process(diff, this))
            .filter(dot -> dot.getDuration() <= 0)
            .forEach(dot -> {
                damageOverTime.remove(dot);
                currentCell.onEvent(this, WorldEventType.DOT_REMOVE, new ByteArray().put(dot.getId()).put(dot.getCaster().getOID()));
            });
    }

    // Effect processors

    public long processEffects(EffectDirection direction, EffectAttribute attribute, long value) {
        final long[] input = new long[] { value };

        List<EffectWrapper> effects = this.effects.stream()
            .filter(effect -> direction.equals(effect.getDirection()))
            .collect(Collectors.toList());

        switch (attribute) {
            case POTENTIAL:
                effects.forEach(effect -> input[0] = effect.getEffect().processPotential(input[0]));
                break;
            case DURATION:
                effects.forEach(effect -> input[0] = effect.getEffect().processDuration(input[0]));
                break;
            case TICK_TIME:
                effects.forEach(effect -> input[0] = effect.getEffect().processTickTime(input[0]));
                break;
            case COOLDOWN:
                effects.forEach(effect -> input[0] = effect.getEffect().processCoolDown(input[0]));
                break;
            case CAST_TIME:
                effects.forEach(effect -> input[0] = effect.getEffect().processCastTime(input[0]));
                break;
            default:
                logger.warn("Unknown effect attribute: {}", attribute);
                break;
        }

        return input[0];
    }
}
