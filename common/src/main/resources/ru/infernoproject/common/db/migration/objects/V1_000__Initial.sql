CREATE TABLE races (
  id       INT(4) PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(50) UNIQUE,
  resource VARCHAR(50) UNIQUE
);

CREATE TABLE classes (
  id       INT(4) PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(50) UNIQUE,
  resource VARCHAR(50) UNIQUE
);

CREATE TABLE locations (
  id   INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE
);

CREATE TABLE items (
  id            INT(11) PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(50) UNIQUE,
  sell_price    INT(11),
  vendor_price  INT(11),
  max_stack     INT(3),
  max_owned     INT(3),
  durability    INT(3),
  allowed_slots VARCHAR(50)
);

CREATE TABLE scripts (
  id     INT(11) PRIMARY KEY AUTO_INCREMENT,
  name   VARCHAR(50) UNIQUE,
  lang   VARCHAR(50),
  script TEXT
);

CREATE TABLE commands (
  id     INT(11) PRIMARY KEY AUTO_INCREMENT,
  name   VARCHAR(50) UNIQUE,
  level  ENUM('user', 'moderator', 'game_master', 'admin'),
  script INT(11),

  CONSTRAINT FOREIGN KEY script_id (script) REFERENCES scripts (id) ON DELETE CASCADE
);

CREATE TABLE spell_effects (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(50) UNIQUE,
  duration        INT(11),
  type            ENUM('buff', 'debuff', 'aura'),
  direction       ENUM('defense', 'offense'),
  script          INT(11),

  CONSTRAINT FOREIGN KEY spell_effect_script_id (script) REFERENCES scripts (id) ON DELETE CASCADE
);

CREATE TABLE spell_damage_over_time_effects (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(50) UNIQUE,
  duration        INT(11),
  tick_interval   INT(11),
  basic_potential INT(11),
  script          INT(11),

  CONSTRAINT FOREIGN KEY spell_damage_over_time_script_id (script) REFERENCES scripts (id) ON DELETE CASCADE
);

CREATE TABLE spells (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(50) UNIQUE,
  type            ENUM('self', 'single_target', 'area_of_effect'),
  required_class  INT(11),
  required_level  INT(11),
  cool_down       INT(11),
  distance        FLOAT,
  radius          FLOAT,
  basic_potential INT(11),
  effect          INT(11),
  dot             INT(11),
  script          INT(11),

  CONSTRAINT FOREIGN KEY spell_script_id (script) REFERENCES scripts (id) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY spell_effect_id (effect) REFERENCES spell_effects (id),
  CONSTRAINT FOREIGN KEY spell_dot_id (dot) REFERENCES spell_damage_over_time_effects (id)
);