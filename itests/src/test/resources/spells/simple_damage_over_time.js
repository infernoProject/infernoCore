var Base = Java.type('ru.infernoproject.worldd.script.impl.DamageOverTimeBase');

var DamageOverTime = Java.extend(Base, {
    tick: function (caster, target, potential) {
        target.processHitPointChange(-potential);
    }
});

var sObject = new DamageOverTime();