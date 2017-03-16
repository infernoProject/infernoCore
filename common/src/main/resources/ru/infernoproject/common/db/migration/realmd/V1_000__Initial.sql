CREATE TABLE accounts (
  id          INT(11) PRIMARY KEY AUTO_INCREMENT,
  login       VARCHAR(50) UNIQUE,
  email       VARCHAR(100) UNIQUE,
  level       ENUM('user', 'moderator', 'game_master', 'admin'),
  salt        VARCHAR(32) NOT NULL,
  verifier    VARCHAR(1024),
  last_login  TIMESTAMP DEFAULT now()
);

CREATE TABLE realm_list (
  id          INT(11) PRIMARY KEY AUTO_INCREMENT,
  online      INT(1),
  last_seen   TIMESTAMP DEFAULT now(),
  name        VARCHAR(50) UNIQUE,
  type        INT(2),
  server_host VARCHAR(128),
  server_port INT(5),

  CONSTRAINT server_address UNIQUE (server_host, server_port)
);

CREATE TABLE sessions (
  id              INT(11) PRIMARY KEY AUTO_INCREMENT,
  account         INT(11) UNIQUE,
  session_key     VARCHAR(32) UNIQUE,
  last_activity   TIMESTAMP DEFAULT now(),
  session_address VARCHAR(21) UNIQUE,
  vector          VARCHAR(64) UNIQUE,

  CONSTRAINT FOREIGN KEY account_id (account) REFERENCES accounts (id) ON DELETE CASCADE
)