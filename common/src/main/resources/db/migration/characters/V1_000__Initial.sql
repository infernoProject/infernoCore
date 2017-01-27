CREATE TABLE characters (
  id             INT(11) PRIMARY KEY AUTO_INCREMENT,
  firstName      VARCHAR(50),
  lastName       VARCHAR(50),
  account        INT(11),
  race           INT(4),
  gender         ENUM('male', 'female'),
  class          INT(4),
  level          INT(4) DEFAULT 1,
  exp            INT(11) DEFAULT 0,
  currency       INT(15) DEFAULT 0,
  max_health     INT(11),
  current_health INT(11),
  max_mana       INT(11),
  current_mana   INT(11),
  body           BLOB,

  CONSTRAINT name UNIQUE (firstName, lastName)
);

CREATE TABLE character_inventory (
  id             INT(11) PRIMARY KEY AUTO_INCREMENT,
  character_id   INT(11) NOT NULL,
  item_id        INT(11) NOT NULL,
  quantity       INT(3) DEFAULT 1,
  durability     INT(3) DEFAULT 100,
  inventory_type INT(1),
  inventory_id   INT(3),

  CONSTRAINT FOREIGN KEY char_id (character_id) REFERENCES characters (id) ON DELETE CASCADE,
  CONSTRAINT item_position UNIQUE (character_id, inventory_type, inventory_id)
);

CREATE TABLE character_spells (
  id           INT(11) PRIMARY KEY AUTO_INCREMENT,
  character_id INT(11),
  spell_id     INT(11),
  cooldown     INT(6),

  CONSTRAINT FOREIGN KEY char_id (character_id) REFERENCES characters (id) ON DELETE CASCADE,
  CONSTRAINT char_spell UNIQUE (character_id, spell_id)
);

CREATE TABLE character_auras (
  id           INT(11) PRIMARY KEY AUTO_INCREMENT,
  character_id INT(11),
  aura_id      INT(11),
  duration     INT(6),

  CONSTRAINT FOREIGN KEY char_id (character_id) REFERENCES characters (id) ON DELETE CASCADE,
  CONSTRAINT char_aura UNIQUE (character_id, aura_id)
);