CREATE TABLE schools
(
    id          INT PRIMARY KEY AUTO_INCREMENT,

    school      VARCHAR(255) NOT NULL,
    city        VARCHAR(255) NOT NULL,

    district_id INT          NOT NULL,
    CONSTRAINT schools_district_id
        FOREIGN KEY (district_id)
            REFERENCES districts (id)
            ON DELETE RESTRICT
            ON UPDATE RESTRICT
) ENGINE InnoDB
  CHAR SET UTF8MB4;
