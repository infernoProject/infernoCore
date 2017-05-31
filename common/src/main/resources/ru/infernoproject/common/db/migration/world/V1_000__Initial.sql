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
  CONSTRAINT vendor_item UNIQUE (vendor_id, item_id)
);