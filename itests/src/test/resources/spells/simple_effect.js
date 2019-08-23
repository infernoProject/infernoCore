var Base = Java.type('ru.infernoproject.worldd.script.impl.EffectBase');

var Effect = Java.extend(Base, {
    processPotential: function (potential) {
        return potential;
    },
    processDuration: function (duration) {
        return duration;
    },
    processTickTime: function (tickTime) {
        return tickTime;
    },
    processCoolDown: function (coolDown) {
        return coolDown;
    },
    processCastTime: function (castTime) {
        return castTime;
    }
});

var sObject = new Effect();