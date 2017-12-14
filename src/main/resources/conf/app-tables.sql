/* order_confirmation_record */
CREATE TABLE IF NOT EXISTS order_confirmation_record (
  id         VARCHAR(40) PRIMARY KEY NOT NULL,
  context    VARCHAR(10)             NOT NULL DEFAULT '',
  uploadTime DATETIME                NOT NULL,
  result     VARCHAR(500)            NOT NULL
);

/* cronjob_record */
CREATE TABLE IF NOT EXISTS cronjob_record (
  id      VARCHAR(40) PRIMARY KEY NOT NULL,
  jobName VARCHAR(50)             NOT NULL DEFAULT '',
  runTime DATETIME                NOT NULL
);


/* order submission record */
CREATE TABLE IF NOT EXISTS order_fulfillment_record (
  id                VARCHAR(40) PRIMARY KEY NOT NULL,
  orderId           VARCHAR(25)             NOT NULL DEFAULT '',
  sku               VARCHAR(100)            NOT NULL DEFAULT '',
  sheetName         VARCHAR(25)             NOT NULL DEFAULT '',
  spreadsheetId     VARCHAR(100)            NOT NULL DEFAULT '',
  purchaseDate      VARCHAR(25)             NOT NULL DEFAULT '',
  isbn              VARCHAR(10)             NOT NULL DEFAULT '',
  seller            VARCHAR(100)            NOT NULL DEFAULT '',
  sellerId          VARCHAR(30)             NOT NULL DEFAULT '',
  sellerPrice       VARCHAR(10)             NOT NULL DEFAULT '',
  condition         VARCHAR(20)             NOT NULL DEFAULT '',
  character         VARCHAR(10)             NOT NULL DEFAULT '',
  remark            VARCHAR(100)            NOT NULL DEFAULT '',
  quantityPurchased INTEGER(3)              NOT NULL DEFAULT 0,
  quantityBought    INTEGER(3)              NOT NULL DEFAULT 0,
  cost              VARCHAR(10)             NOT NULL DEFAULT '',
  orderNumber       VARCHAR(25)             NOT NULL DEFAULT '',
  buyerAccount      VARCHAR(100)            NOT NULL DEFAULT '',
  lastCode          VARCHAR(10)             NOT NULL DEFAULT '',
  fulfillDate       DATETIME                NOT NULL
);

/* order submission record */
CREATE TABLE IF NOT EXISTS order_fulfillment_logs (
  id                VARCHAR(40) PRIMARY KEY NOT NULL,
  orderId           VARCHAR(25)             NOT NULL DEFAULT '',
  sku               VARCHAR(100)            NOT NULL DEFAULT '',
  sheetName         VARCHAR(25)             NOT NULL DEFAULT '',
  spreadsheetId     VARCHAR(100)            NOT NULL DEFAULT '',
  purchaseDate      VARCHAR(25)             NOT NULL DEFAULT '',
  isbn              VARCHAR(10)             NOT NULL DEFAULT '',
  seller            VARCHAR(100)            NOT NULL DEFAULT '',
  sellerId          VARCHAR(30)             NOT NULL DEFAULT '',
  sellerPrice       VARCHAR(10)             NOT NULL DEFAULT '',
  condition         VARCHAR(20)             NOT NULL DEFAULT '',
  character         VARCHAR(10)             NOT NULL DEFAULT '',
  remark            VARCHAR(100)            NOT NULL DEFAULT '',
  quantityPurchased INTEGER(3)              NOT NULL DEFAULT 0,
  quantityBought    INTEGER(3)              NOT NULL DEFAULT 0,
  cost              VARCHAR(10)             NOT NULL DEFAULT '',
  orderNumber       VARCHAR(25)             NOT NULL DEFAULT '',
  buyerAccount      VARCHAR(100)            NOT NULL DEFAULT '',
  lastCode          VARCHAR(10)             NOT NULL DEFAULT '',
  fulfilledAddress  TEXT                    NOT NULL DEFAULT '',
  shippingAddress   TEXT                    NOT NULL DEFAULT '',
  fulfilledASIN     VARCHAR(10)             NOT NULL DEFAULT '',
  fulfillDate       DATETIME                NOT NULL
);


INSERT INTO order_fulfillment_logs (id, orderId, sku, sheetName, spreadsheetId, purchaseDate, isbn, seller, sellerId,
                                    sellerPrice, condition, character, remark, quantityPurchased, quantityBought, cost, orderNumber, buyerAccount, lastCode, fulfillDate)
  SELECT *
  FROM order_fulfillment_record;

/* bug */
CREATE TABLE IF NOT EXISTS bug (
  id         VARCHAR(25) PRIMARY KEY NOT NULL,
  issue      VARCHAR(25)             NOT NULL,
  level      VARCHAR(25)             NOT NULL,
  orderId    VARCHAR(25)             NOT NULL,
  title      VARCHAR(500)            NOT NULL,
  reportTime DATETIME                NOT NULL
);

/* order submission record */
CREATE TABLE IF NOT EXISTS order_submission_tasks (
  id                VARCHAR(40) PRIMARY KEY NOT NULL,
  sid               VARCHAR(5)              NOT NULL,
  marketplaceName   VARCHAR(2)              NOT NULL,
  spreadsheetId     VARCHAR(100)            NOT NULL,
  spreadsheetName   VARCHAR(255)            NOT NULL,
  orderRangeCol     VARCHAR(255)            NOT NULL,
  lostLimit         VARCHAR(3)              NOT NULL,
  priceLimit        VARCHAR(3)              NOT NULL,
  eddLimit          VARCHAR(3)              NOT NULL,
  noInvoiceText     VARCHAR(30)             NOT NULL,
  skipValidationCol VARCHAR(255)            NOT NULL,
  finderCode        VARCHAR(255)            NOT NULL,
  status            VARCHAR(255)            NOT NULL,
  orders            TEXT                    NULL,
  invalidOrders     TEXT                    NULL,
  summary           TEXT                    NULL,
  totalOrders       INT(5)                  NULL,
  success           INT(5)                  NULL,
  failed            INT(5)                  NULL,
  timeTaken         VARCHAR(255)            NULL,
  dateCreated       DATETIME                NOT NULL,
  dateStarted       DATETIME                NULL,
  dateEnded         DATETIME                NULL
);