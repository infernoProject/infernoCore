CREATE TABLE characters (
  id INT(11) PRIMARY KEY AUTO_INCREMENT,

  firstName VARCHAR(50),
  lastName VARCHAR(50),

  account INT(11),

  race INT(4),
  gender ENUM('male', 'female'),
  class INT(4),

  level INT(4) DEFAULT 0,
  exp INT(11) DEFAULT 0,
  currency INT(15) DEFAULT 0,

  body BLOB,

  CONSTRAINT name UNIQUE (firstName, lastName)
);