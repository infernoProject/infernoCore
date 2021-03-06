CREATE TABLE characters (
  id             INT(11) PRIMARY KEY AUTO_INCREMENT,
  first_name     VARCHAR(50),
  last_name      VARCHAR(50),

  account        INT(11),
  realm          INT(11),

  race           INT(4),
  gender         ENUM('male', 'female'),
  class          INT(4),

  level          INT(4) DEFAULT 1,
  exp            INT(11) DEFAULT 0,
  currency       INT(15) DEFAULT 0,

  location       INT(11),
  position_x     FLOAT,
  position_y     FLOAT,
  position_z     FLOAT,
  orientation    FLOAT,

  max_health     INT(11),
  current_health INT(11),
  max_mana       INT(11),
  current_mana   INT(11),

  delete_flag    INT(1) DEFAULT 0,
  delete_after   TIMESTAMP NULL,

  body           TEXT,

  CONSTRAINT character_name UNIQUE (first_name, last_name, realm, delete_flag, delete_after)
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

CREATE TABLE guilds (
  id           INT(11) PRIMARY KEY AUTO_INCREMENT,
  realm        INT(11),
  tag          VARCHAR(6),
  title        VARCHAR(40),
  description  TEXT,

  CONSTRAINT realm_guild_tag UNIQUE (realm, tag),
  CONSTRAINT realm_guild_title UNIQUE (realm, title)
);

CREATE TABLE guild_members (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  guild_id INT(11),
  character_id INT(11) UNIQUE,
  level INT(1),

  CONSTRAINT FOREIGN KEY gd_char_id (character_id) REFERENCES characters (id) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY gd_id (guild_id) REFERENCES guilds (id) ON DELETE CASCADE
)