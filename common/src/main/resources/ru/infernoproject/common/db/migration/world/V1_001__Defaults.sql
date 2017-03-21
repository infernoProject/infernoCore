INSERT INTO races (id, name, resource) VALUES
  (1, 'Human', 'Human');

INSERT INTO classes (id, name, resource) VALUES
  (1, 'Arcanist', 'acn');

INSERT INTO items (id, name, sell_price, vendor_price, max_stack, max_owned, durability) VALUES
  (1, 'Test Item', 10, 1, 100, 100, 100),
  (2, 'Test Unique Item', 10, 1, 1, 1, 100);

INSERT INTO vendors (id, name, map, position_x, position_y, position_z) VALUES
  (1, 'Vendor', 1, 5.0, 5.0, 0.0);

INSERT INTO vendor_items (id, vendor_id, item_id, quantity) VALUES
  (1, 1, 1, 100),
  (2, 1, 2, 10);

INSERT INTO scripts (id, name, type, script) VALUES
  (1, 'spell_attack', 1, 'from ru.infernoproject.worldd.scripts.impl import Spell

class AttackSpell(Spell):

    def cast(self, source, targets):
        for target in targets:
            target.processDamage(source, self.getPotency())

spell = AttackSpell()'),
  (2, 'spell_heal', 1, 'from ru.infernoproject.worldd.scripts.impl import Spell

class HealSpell(Spell):

    def cast(self, source, targets):
        for target in targets:
            target.processHeal(source, self.getPotency())

spell = HealSpell()'),
  (3, 'spell_revive', 1, 'from ru.infernoproject.worldd.scripts.impl import Spell

class ReviveSpell(Spell):

    def cast(self, source, targets):
        for target in targets:
            target.processRevive(source)

spell = ReviveSpell()'),
  (4, 'spell_buff', 1, 'from ru.infernoproject.worldd.scripts.impl import Spell

class AuraSpell(Spell):

    def cast(self, source, targets):
        for target in targets:
            aura = self.getScriptManager().auraGet(1).getAura(self.getScriptManager())
            target.processAura(source, aura)

spell = AuraSpell()'),
  (5, 'aura_heal', 2, 'from ru.infernoproject.worldd.scripts.impl import Aura

class HealAura(Aura):

    def tick(self, source, target):
        target.processHeal(source, self.getPotency())

aura = HealAura()'),
  (6, 'command_learn', 3, 'from ru.infernoproject.worldd.scripts.impl import Command
import re

class LearnCommand(Command):

    def execute(self, args):
        char = self.getSession().getPlayer().getCharacterInfo()
        result = []
        for arg in args:
            if re.match(\'^([0-9]+)$\', arg):
                spell_id = int(arg)
                if self.getCharacterManager().spellLearned(char, spell_id):
                    result.append("%s: already learned" % spell_id)
                else:
                    if self.getCharacterManager().spellLearn(char, spell_id):
                        result.append("%s: learned" % spell_id)
                    else:
                        result.append("%s: not exists" % spell_id)
        return 0, result

command = LearnCommand()');

INSERT INTO spells (id, name, potency, radius, distance, cooldown, script) VALUES
  (1, 'Attack', 10, 0.0, 0.0, 1000, 1),
  (2, 'Heal', 10, 0.0, 0.0, 1000, 2),
  (3, 'Revive', 0, 0.0, 0.0, 1000, 3),
  (4, 'Buff', 0, 0.0, 0.0, 1000, 4);

INSERT INTO auras (id, name, potency, tick_interval, duration, script) VALUES
  (1, 'Heal Aura', 10, 1000, 15000, 5);

INSERT INTO commands (name, level, script) VALUES
  ('learn', 'game_master', 6);