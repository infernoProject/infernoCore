CREATE TABLE races (
  id INT(4) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE,
  resource VARCHAR(50) UNIQUE
);

CREATE TABLE classes (
  id INT(4) PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) UNIQUE,
  resource VARCHAR(50) UNIQUE
);