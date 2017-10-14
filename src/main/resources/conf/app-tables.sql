
/* order_confirmation_record */
CREATE TABLE IF NOT EXISTS order_confirmation_record (
  id         VARCHAR(40) PRIMARY KEY NOT NULL,
  context    VARCHAR(10)             NOT NULL DEFAULT '',
  uploadTime DATETIME                NOT NULL,
  result     VARCHAR(500)            NOT NULL
);

/* cronjob_record */
CREATE TABLE IF NOT EXISTS cronjob_record (
  id         VARCHAR(40) PRIMARY KEY NOT NULL,
  jobName    VARCHAR(50)             NOT NULL DEFAULT '',
  runTime    DATETIME                NOT NULL
);