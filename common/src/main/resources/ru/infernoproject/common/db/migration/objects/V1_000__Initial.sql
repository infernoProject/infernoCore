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
  id           INT(11) PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(50) UNIQUE,
  sell_price   INT(11),
  vendor_price INT(11),
  max_stack    INT(3),
  max_owned    INT(3),
  durability   INT(3)
);

CREATE TABLE scripts (
  id     INT(11) PRIMARY KEY AUTO_INCREMENT,
  name   VARCHAR(50) UNIQUE,
  script TEXT
);

CREATE TABLE commands (
  id     INT(11) PRIMARY KEY AUTO_INCREMENT,
  name   VARCHAR(50) UNIQUE,
  level  ENUM('user', 'moderator', 'game_master', 'admin'),
  script INT(11),

  CONSTRAINT FOREIGN KEY script_id (script) REFERENCES scripts (id) ON DELETE CASCADE
);