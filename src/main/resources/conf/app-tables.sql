
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


/* order submission record */
CREATE TABLE IF NOT EXISTS order_fulfillment_record (
  id         VARCHAR(40) PRIMARY KEY NOT NULL,
  orderId    VARCHAR(25)             NOT NULL DEFAULT '',
  sku    VARCHAR(100)             NOT NULL DEFAULT '',
  sheetName    VARCHAR(25)             NOT NULL DEFAULT '',
  spreadsheetId VARCHAR(100)             NOT NULL DEFAULT '',
  purchaseDate    VARCHAR(25)             NOT NULL DEFAULT '',
  isbn   VARCHAR(10)             NOT NULL DEFAULT '',
  seller   VARCHAR(100)             NOT NULL DEFAULT '',
  sellerId   VARCHAR(30)             NOT NULL DEFAULT '',
  sellerPrice   VARCHAR(10)             NOT NULL DEFAULT '',
  condition   VARCHAR(20)             NOT NULL DEFAULT '',
  character   VARCHAR(10)             NOT NULL DEFAULT '',
  remark   VARCHAR(100)             NOT NULL DEFAULT '',
  quantityPurchased INTEGER(3) NOT NULL DEFAULT 0,
  quantityBought INTEGER(3) NOT NULL DEFAULT 0,
  cost   VARCHAR(10)             NOT NULL DEFAULT '',
  orderNumber    VARCHAR(25)             NOT NULL DEFAULT '',
  buyerAccount    VARCHAR(100)             NOT NULL DEFAULT '',
  lastCode    VARCHAR(10)             NOT NULL DEFAULT '',
  fulfillAddress TEXT    NOT NULL DEFAULT '',
  shippingAddress TEXT    NOT NULL DEFAULT '',
  fulfilledASIN    VARCHAR(10)             NOT NULL DEFAULT '',
  fulfillDate    DATETIME                NOT NULL
);