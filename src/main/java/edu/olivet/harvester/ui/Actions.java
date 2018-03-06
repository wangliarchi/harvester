package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.Action;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:58 AM
 */
@SuppressWarnings("CheckStyle")
public class Actions {

    public static final Action SubmitOrder = new Action("SubmitOrder", "Submit Orders", null, "start.png");

    //    public static final Action DuplicatedOrders = new Action("DuplicatedOrders", "Duplicated Orders", null, "bug.png");
    public static final Action OrderStatisticLog = new Action("OrderStatisticLog", "Order Statistic Logs", null, "log.png");
    public static final Action OrderSuccessLog = new Action("OrderSuccessLog", "Order Success Logs", null, "log.png");

    public static final Action AddOrderTask = new Action("AddOrderTask", "Create Order Submission Task", null, "log.png");


    public static final Action OrderSubmissionLog = new Action("OrderSubmissionLog", "Order Submission Logs", null, "list.png");

    public static final Action OrderSubmissionTasks = new Action("OrderSubmissionTasks", "Order Submission Tasks", null, "list.png");

    public static final Action FindSupplier = new Action("FindSupplier", "Find Suppliers", null, "find.png");

    public static final Action ConfirmShipment = new Action("ConfirmShipment", "Confirm Shipments", null, "truck.png");

    public static final Action OrderConfirmationHistory = new Action("OrderConfirmationHistory", "Order Confirmation History", null, "list.png");

    public static final Action ConfigBankCard = new Action("ConfigBankCard", "Bank Card Configuration", null, "creditcards.png");

    public static final Action BuyerAccountSettings = new Action("ConfigBuyerAccount", "Buyer Account Configuration", null, "follow.png");


    public static final Action ReportBug = new Action("ReportBug", "Report a Bug", null, "bug.png");

    public static final Action ExportOrders = new Action("ExportOrders", "Export Orders", null, "ordersheet.png");

    public static final Action SystemSettings = new Action("SystemSettings", "System Settings", null, "settings.gif");

    public static final Action TitleChecker = new Action("TitleChecker", "Product Title Checker", null, null);

    public static final Action OrderChecker = new Action("OrderChecker", "Order Data Checker", null, null);

    public static final Action OrderFulfillmentChecker = new Action("OrderFulfillmentChecker", "Order Fulfillment Checker", null, null);

    public static final Action DownloadInvoice = new Action("DownloadInvoice", "Download Order Invoices", null, "invoice.png");

    public static final Action SyncASINs = new Action("SyncASINs", "Sync ASINs", null, null);

    public static final Action DownloadInventory = new Action("DownloadInventory", "Download Inventory", null, null);

    public static final Action SubmitSelfOrders = new Action("SubmitSelfOrders", "Submit Self Orders", null, "start.png");
}
