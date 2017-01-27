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

CREATE TABLE items (
  id           INT(11) PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(50) UNIQUE,
  sell_price   INT(11),
  vendor_price INT(11),
  max_stack    INT(3),
  max_owned    INT(3),
  durability   INT(3)
);

CREATE TABLE spells (
  id       INT(11) PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(50) UNIQUE,
  potency  INT(11),
  radius   DOUBLE,
  distance DOUBLE,
  cooldown INT(6),
  script   TEXT
);

CREATE TABLE auras (
  id            INT(11) PRIMARY KEY AUTO_INCREMENT,
  name          VARCHAR(50) UNIQUE,
  potency       INT(11),
  tick_interval INT(11),
  duration      INT(6),
  script        TEXT
);

CREATE TABLE vendors (
  id         INT(11) PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(50) UNIQUE,
  map        INT(11),
  position_x DOUBLE,
  position_y DOUBLE,
  position_z DOUBLE
);

CREATE TABLE vendor_items (
  id        INT(11) PRIMARY KEY AUTO_INCREMENT,
  vendor_id INT(11),
  item_id   INT(11),
  quantity  INT(3),

  CONSTRAINT FOREIGN KEY vendor (vendor_id) REFERENCES vendors (id) ON DELETE CASCADE,
  CONSTRAINT FOREIGN KEY item (item_id) REFERENCES items (id) ON DELETE CASCADE,
  CONSTRAINT vendor_item UNIQUE (vendor_id, item_id)
);

CREATE TABLE commands (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE,
  level ENUM('user', 'moderator', 'game_master', 'admin'),
  script TEXT
);