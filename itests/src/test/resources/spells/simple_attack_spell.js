var Base = Java.type('ru.infernoproject.worldd.script.impl.SpellBase');
var LoggerFactory = Java.type('org.slf4j.LoggerFactory');

var Spell = Java.extend(Base, {
  cast: function (helper, caster, target, potential) {
      var logger = LoggerFactory.getLogger(Base.class);

      target.processHitPointChange(-potential);

      logger.info('{} casts spell with potential {} to {}', caster, potential, target);
  }
});

var sObject = new Spell();