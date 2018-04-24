package edu.olivet.harvester.ui.menu;

import edu.olivet.foundations.ui.AbstractUIContainer;
import edu.olivet.foundations.ui.Action;
import edu.olivet.foundations.ui.Menu;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 11:59 AM
 */
public class UIElements extends AbstractUIContainer {
    private static final UIElements instance = new UIElements();

    public static UIElements getInstance() {
        return instance;
    }

    private UIElements() {
    }

    private final Menu harvester = new Menu("Harvester", "H");
    private final Menu submitOrder = new Menu("Submit Order", "O");
    private final Menu confirmShipment = new Menu("Confirm Shipment", "C");
    private final Menu finance = new Menu("Finance", "F");
    private final Menu selforder = new Menu("Self Order", "S");

    @Override
    public Menu[] getMenus() {
        return new Menu[] {
                harvester,
                submitOrder,
                confirmShipment,
                Menu.Settings,
                selforder,
                finance,
                Menu.ToolBox,
                Menu.Help
        };
    }

    @Override
    public Map<Menu, Action[]> getMenuActions() {
        Map<Menu, Action[]> map = new HashMap<>();
        map.put(harvester, new Action[] {
                Action.CurrentVersion,
                Action.UpgradeCheck,
                Action.Separator,
                Action.Restart

        });

        map.put(submitOrder, new Action[] {
                //Actions.DuplicatedOrders,
                Actions.AddOrderTask,
                Action.Separator,
                Actions.OrderSubmissionTasks,
                Actions.OrderSubmissionLog,
                Action.Separator,
                Actions.OrderSuccessLog,
                Actions.OrderStatisticLog,
                Action.Separator,
                Actions.CloseAllWebTabs
        });

        map.put(confirmShipment, new Action[] {
                Actions.ConfirmShipment,
                Action.Separator,
                Actions.OrderConfirmationHistory
        });

        map.put(selforder, new Action[] {
                Actions.SubmitSelfOrders,
                Action.Separator,
                Actions.AsyncSelfOrderStats,
                Actions.AddSelfOrderProduct,
                //Actions.ReloadSelfOrderProduct,
                Actions.PostSelfOrderFeedbacks,
                Action.Separator,
                Actions.SystemSettings
        });
        map.put(finance, new Action[] {
                Actions.DownloadInvoice,
                Actions.RunDownloadInvoiceTask,
                Action.Separator,
                Actions.InvoiceTasks,
                Actions.InvoiceDownloadStats,
                Actions.FetchUnprocessedOrders

        });

        map.put(Menu.ToolBox, new Action[] {
                Actions.TitleChecker,
                Actions.CheckStoreName,
                Action.Separator,
                Actions.OrderFulfillmentChecker,
                Action.Separator,
                Actions.DownloadInventory,
                Actions.SyncASINs,
                Action.Separator,
                Actions.CheckPrimeBuyerAccount,
                Actions.FetchTrackingNumbers
        });

        map.put(Menu.Settings, new Action[] {
                Action.Settings,
                Actions.SystemSettings,
                Actions.BuyerAccountSettings,
                Actions.ConfigBankCard,
                Action.Separator,
                Action.CreateAutoStartTask,
                Action.DeleteAutoStartTask,
                Action.CreateShortCut

        });
        map.put(Menu.Help, new Action[] {
                Actions.ReportBug,
                Action.Documentation
        });
        return map;
    }

    @Override
    public Map<Action, Action[]> getActionRelationships() {
        return new HashMap<>();
    }

    @Override
    public Action[] getToolbarActions() {
        return new Action[] {
                Action.Settings,
                Actions.SystemSettings,
                Actions.ExportOrders,
                Actions.ConfirmShipment,
                Actions.FindSupplier,
                Actions.SubmitOrder,
                Actions.CommonLetters,
                Actions.SubmitSelfOrders,
                Actions.ReportBug
        };
    }

    @Override
    public Action getAction(String command) {
        return this.getActionByCommand(command, Action.class, Actions.class);
    }


}
