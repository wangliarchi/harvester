package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.Action;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:58 AM
 */
@SuppressWarnings("CheckStyle")
public class Actions {

    public static final Action SubmitOrder = new Action("SubmitOrder", "Submit Orders", null, "start.png");

    public static final Action FindSupplier = new Action("FindSupplier", "Find Suppliers", null, "find.png");

    public static final Action ConfirmShipment = new Action("ConfirmShipment", "Confirm Shipments", null, "truck.png");

    public static final Action OrderConfirmationHistory = new Action("OrderConfirmationHistory", "Order Confirmation History", null, "list.png");

    public static final Action ConfigBankCard = new Action("ConfigBankCard","Bank Card Configuration",null,"creditcards.png");
}
