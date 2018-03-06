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

/* order submission logs */
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

/* order submission record new */
CREATE TABLE IF NOT EXISTS order_submission_tasks_new (
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
  buyerAccount      VARCHAR(255)            NOT NULL,
  primeBuyerAccount VARCHAR(255)            NOT NULL,
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


INSERT INTO order_submission_tasks_new (id, sid, marketplaceName, spreadsheetId, spreadsheetName, orderRangeCol, lostLimit, priceLimit,
                                        eddLimit, noInvoiceText, skipValidationCol, finderCode, status, orders, invalidOrders, summary, totalOrders,
                                        success, failed, timeTaken, dateCreated, dateStarted, dateEnded)
  SELECT *
  FROM order_submission_tasks;

/* order submission record by buyer account */
CREATE TABLE IF NOT EXISTS order_submission_tasks_by_buyer_accounts (
  id                 VARCHAR(100) PRIMARY KEY NOT NULL,
  taskId             VARCHAR(40)              NOT NULL,
  fulfillmentCountry VARCHAR(2)               NOT NULL,
  buyerAccount       VARCHAR(255)             NOT NULL,
  marketplaceName    VARCHAR(2)               NOT NULL,
  spreadsheetId      VARCHAR(100)             NOT NULL,
  spreadsheetName    VARCHAR(255)             NOT NULL,
  sheetName          VARCHAR(255)             NOT NULL,
  status             VARCHAR(255)             NOT NULL,
  orders             TEXT                     NULL,
  summary            TEXT                     NULL,
  totalOrders        INT(5)                   NULL,
  success            INT(5)                   NULL,
  failed             INT(5)                   NULL,
  timeTaken          VARCHAR(255)             NULL,
  dateCreated        DATETIME                 NOT NULL,
  dateStarted        DATETIME                 NULL,
  dateEnded          DATETIME                 NULL
);

/* amazon_orders */
CREATE TABLE IF NOT EXISTS amazon_orders (
  orderId      VARCHAR  NOT NULL,
  orderItemId  VARCHAR  NOT NULL,
  asin         VARCHAR  NOT NULL,
  sku          VARCHAR  NOT NULL,
  orderStatus  VARCHAR  NOT NULL,
  purchaseDate DATETIME NOT NULL,
  xml          VARCHAR  NOT NULL,
  itemXml      VARCHAR  NOT NULL,
  isbn         VARCHAR  NOT NULL,
  exportStatus INTEGER  NOT NULL,
  lastUpdate   DATETIME NOT NULL,
  PRIMARY KEY (orderItemId)
    ON CONFLICT IGNORE
);

/* amazon_order_logs */
CREATE TABLE IF NOT EXISTS amazon_order_logs (
  orderId      VARCHAR  NOT NULL,
  orderItemId  VARCHAR  NOT NULL,
  asin         VARCHAR  NOT NULL,
  sku          VARCHAR  NOT NULL,
  orderStatus  VARCHAR  NOT NULL,
  purchaseDate DATETIME NOT NULL,
  xml          VARCHAR  NOT NULL,
  itemXml      VARCHAR  NOT NULL,
  isbn         VARCHAR  NOT NULL,
  name         VARCHAR  NOT NULL,
  email        VARCHAR  NOT NULL,
  exportStatus INTEGER  NOT NULL,
  lastUpdate   DATETIME NOT NULL,
  PRIMARY KEY (orderItemId)
    ON CONFLICT IGNORE
);
INSERT INTO amazon_order_logs (orderId, orderItemId, asin, sku, orderStatus, purchaseDate, xml, itemXml,
                               isbn, exportStatus, lastUpdate)
  SELECT *
  FROM amazon_orders;

/* amazon_orders_new */
CREATE TABLE IF NOT EXISTS amazon_orders_new (
  orderId      VARCHAR  NOT NULL,
  orderItemId  VARCHAR  NOT NULL,
  asin         VARCHAR  NOT NULL,
  sku          VARCHAR  NOT NULL,
  orderStatus  VARCHAR  NOT NULL,
  purchaseDate DATETIME NOT NULL,
  xml          VARCHAR  NOT NULL,
  itemXml      VARCHAR  NOT NULL,
  isbn         VARCHAR  NULL,
  name         VARCHAR  NULL,
  email        VARCHAR  NULL,
  exportStatus INTEGER  NULL,
  lastUpdate   DATETIME NULL,
  PRIMARY KEY (orderItemId)
    ON CONFLICT IGNORE
);

INSERT INTO amazon_orders_new
  SELECT *
  FROM amazon_order_logs;

/* buyer_invoices */
CREATE TABLE IF NOT EXISTS buyer_invoices (
  buyerEmail     VARCHAR  NOT NULL,
  country        VARCHAR  NOT NULL,
  orderId        VARCHAR  NOT NULL,
  cardNo         VARCHAR  NOT NULL,
  orderTotal     FLOAT    NOT NULL,
  purchaseDate   DATETIME NOT NULL,
  dateDownloaded DATETIME NULL,
  PRIMARY KEY (orderId)
    ON CONFLICT IGNORE
);

/* order_inventory_loader */
CREATE TABLE IF NOT EXISTS order_inventory_loader (
  id         VARCHAR(120) PRIMARY KEY NOT NULL,
  orderId    VARCHAR(25)              NOT NULL,
  sku        VARCHAR(100)             NOT NULL,
  updateType VARCHAR(50)              NOT NULL
);

/* self_orders */
CREATE TABLE IF NOT EXISTS self_orders (
  id               VARCHAR(40) PRIMARY KEY NOT NULL,
  sheetName        VARCHAR(25)             NOT NULL DEFAULT '',
  spreadsheetId    VARCHAR(100)            NOT NULL DEFAULT '',
  sellerCode       VARCHAR(100)            NOT NULL DEFAULT '',
  seller           VARCHAR(100)            NOT NULL DEFAULT '',
  sellerId         VARCHAR(30)             NOT NULL DEFAULT '',
  primoCode        VARCHAR(100)            NOT NULL DEFAULT '',
  country          VARCHAR(30)             NOT NULL DEFAULT '',
  asin             VARCHAR(10)             NOT NULL DEFAULT '',
  cost             VARCHAR(10)             NOT NULL DEFAULT '',
  orderNumber      VARCHAR(25)             NOT NULL DEFAULT '',
  buyerAccount     VARCHAR(100)            NOT NULL DEFAULT '',
  fulfilledAddress TEXT                    NOT NULL DEFAULT '',
  fulfillDate      DATETIME                NOT NULL
);