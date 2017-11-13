package edu.olivet.harvester.fulfill;


import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.RuntimeSettings;
import edu.olivet.harvester.fulfill.service.MarkStatusService;
import edu.olivet.harvester.fulfill.service.OrderFlowEngine;
import edu.olivet.harvester.fulfill.service.SheetService;
import edu.olivet.harvester.fulfill.utils.DailyBudgetHelper;
import edu.olivet.harvester.fulfill.utils.FulfillmentEnum;
import edu.olivet.harvester.fulfill.utils.OrderValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.OrderService;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.ui.BuyerPanel;
import edu.olivet.harvester.ui.TabbedBuyerPanel;
import edu.olivet.harvester.utils.MessageListener;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Order station prototype entry
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);


    @Inject
    AppScript appScript;
    @Inject
    SheetService sheetService;
    @Inject
    OrderValidator orderValidator;

    @Inject
    MarkStatusService markStatusService;

    @Inject
    OrderFlowEngine orderFlowEngine;

    @Inject
    MessageListener messageListener;

    @Inject
    DailyBudgetHelper dailyBudgetHelper;

    @Inject
    OrderService orderService;

    public void execute(RuntimeSettings settings) {

        try {
            String spreadsheetId = settings.getSpreadsheetId();
            dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());
        } catch (Exception e) {
            UITools.error(e.getMessage());
            return;
        }

        //check duplication
        Spreadsheet spreadsheet = sheetService.getSpreadsheet(settings.getSpreadsheetId());
        List<Order> duplicatedOrders = orderService.findDuplicates(spreadsheet);
        if (CollectionUtils.isNotEmpty(duplicatedOrders)) {
            String msg = String.format("%s duplicated orders found in %s, %s", duplicatedOrders.size(), spreadsheet.getProperties().getTitle(),
                    StringUtils.join(duplicatedOrders.stream().map(it -> it.order_id + " @ " + it.sheetName).collect(Collectors.toSet()).toArray(new String[duplicatedOrders.size()]), ", "));
            UITools.error(msg + "\n\n Please fix before submitting orders.");
            return;
        }

        long start = System.currentTimeMillis();


        //mark status first
        markStatusService.excute(settings, true);


        List<Order> orders = appScript.readOrders(settings);

        String resultSummary = String.format("Finished loading orders to submit for %s, %d orders found, took %s", settings.toString(), orders.size(), Strings.formatElapsedTime(start));
        LOGGER.info(resultSummary);
        messageListener.addLongMsg(resultSummary, orders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);

        if (CollectionUtils.isEmpty(orders)) {
            UITools.info(UIText.message("message.info.nostatus"), UIText.title("title.result"));
            return;
        }

        //remove if not valid
        List<Order> validOrders = new ArrayList<>();
        for (Order order : orders) {
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error, InformationLevel.Negative);
            } else {
                validOrders.add(order);
            }
        }

        if (CollectionUtils.isEmpty(validOrders)) {
            LOGGER.info("No valid orders to submit.");
            UITools.error("No valid orders to be submitted. See failed record log for more detail.");
            messageListener.addMsg("No valid orders to be submited.");
            return;
        }


        if (!UITools.confirmed(UIText.message("message.info.statusupdated", validOrders.size(), Strings.formatElapsedTime(start)))) {
            return;
        }

        resultSummary = String.format("%d order(s) to be submitted.", validOrders.size());
        LOGGER.info(resultSummary);
        messageListener.addMsg(resultSummary, validOrders.size() > 0 ? InformationLevel.Information : InformationLevel.Negative);


        for (Order order : validOrders) {
            try {
                Account buyer = OrderHelper.getBuyer(order);
                Country country = OrderHelper.getFulfillementCountry(order);
                BuyerPanel buyerPanel = TabbedBuyerPanel.getInstance().getOrAddTab(country, buyer);
                TabbedBuyerPanel.getInstance().highlight(buyerPanel);
                submit(order, buyerPanel);
            } catch (Exception e) {
                LOGGER.error("Error submit order {}", order.order_id, e);
                messageListener.addMsg(order, e.getMessage(), InformationLevel.Negative);
            }
        }

    }

    public void submit(Order order, BuyerPanel buyerPanel) {
        String spreadsheetId = RuntimeSettings.load().getSpreadsheetId();

        try {

            //validate again!
            String error = orderValidator.isValid(order, FulfillmentEnum.Action.SubmitOrder);
            if (StringUtils.isNotBlank(error)) {
                messageListener.addMsg(order, error);
                return;
            }
            dailyBudgetHelper.getRemainingBudget(spreadsheetId, new Date());

            messageListener.addMsg(order, String.format("start submitting. Buyer account %s, marketplace %s", buyerPanel.getBuyer(), buyerPanel.getCountry().baseUrl()));
            orderFlowEngine.process(order, buyerPanel);
            messageListener.addMsg(order, "order fulfilled successfully.");
        } catch (Exception e) {
            LOGGER.error("Error submit order {}", order.order_id, e);

            Pattern pattern = Pattern.compile(Pattern.quote("xception:"));
            String[] parts = pattern.split(e.getMessage());
            String msg = parts[parts.length - 1].trim();

            messageListener.addMsg(order, msg, InformationLevel.Negative);
            sheetService.fillUnsuccessfulMsg(spreadsheetId, order, msg);
        }

    }

    public static void main(String[] args) {
        UITools.setTheme();

        Order order = new Order();
        order.row = 1;
        order.status = "n";
        order.order_id = "002-1578027-1397838";
        order.recipient_name = "Nicholas Adamo";
        order.purchase_date = "10/24/2014 21:00:00";
        order.sku_address = "https://sellercentral.amazon.com/myi/search/OpenListingsSummary?keyword=new18140915a160118";
        order.sku = "new18140915a160118";
        order.price = "14.48";
        order.quantity_purchased = "2";
        order.shipping_fee = "16.95";
        order.ship_state = "NY";
        order.isbn_address = "http://www.amazon.com/dp/0545521378";
        order.isbn = "0545521378";
        //order.seller = "AP";
        order.seller = "BigHeartedBooks";
        order.seller_id = "A1CRANICB1QVV0";
        order.seller_price = "4.94";
        order.url = "/";
        order.condition = "Used - Good";
        order.character = "pr";
        order.remark = "无Remark";
        order.reference = "1.018";
        order.code = "29";
        order.profit = "7.488";
        order.item_name = "[ NOWHERE TO RUN (39 CLUES: UNSTOPPABLE #01) ] By Watson. Jude ( Author) 2013...";
        order.ship_address_1 = "Ernst  Young";
        order.ship_address_2 = "836 Berkshire Road";
        order.ship_city = "Wingdale";
        order.ship_zip = "12594";
        order.ship_phone_number = "123456";
        order.cost = "20.42";
        order.order_number = "102-0780405-2545043";
        order.account = "joshjohnsonsf007@gmail.com";
        order.last_code = "10.48";
        order.setShip_country("United States");
        order.sales_chanel = "Amazon.com";

        JFrame frame = new JFrame("Order Submission Demo");
        BuyerPanel buyerPanel = new BuyerPanel(order);
        frame.getContentPane().add(buyerPanel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1260, 736));


        ApplicationContext.getBean(OrderSubmitter.class).submit(order, buyerPanel);
    }

}
