CREATE TABLE district
(
    id            INT PRIMARY KEY AUTO_INCREMENT,
    creation_time DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       DATETIME,

    name          VARCHAR(255) UNIQUE NOT NULL
) ENGINE InnoDB
  CHAR SET UTF8MB4;
