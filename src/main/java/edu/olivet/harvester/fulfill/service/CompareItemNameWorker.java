package edu.olivet.harvester.fulfill.service;

import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.utils.ISBNUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 11:00 AM
 */
public class CompareItemNameWorker extends SwingWorker<List<ItemCompareResult>, Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompareItemNameWorker.class);
    private final List<Order> orders;

    public CompareItemNameWorker(List<Order> orders) {
        super();
        this.orders = orders;
    }

    @Override
    protected List<ItemCompareResult> doInBackground() throws Exception {
        Thread.currentThread().setName("CompareItemName");

        List<ItemCompareResult> results = new ArrayList<>(orders.size());
        ItemValidator itemValidator = ApplicationContext.getBean(ItemValidator.class);
        for (Order order : orders) {
            String title = ISBNUtils.getTitle(OrderCountryUtils.getFulfillementCountry(order), order.isbn);
            if (StringUtils.isBlank(title)) {
                continue;
            }

            String itemName = order.item_name.trim();
            edu.olivet.harvester.fulfill.service.ItemValidator.ValidateReport report = itemValidator.validateItemName(title, itemName);
            results.add(new ItemCompareResult(order, title, report.pass, false, report.toString()));
        }

        LOGGER.debug("共计{}条订单，有效书名比较结果{}条", orders.size(), results.size());
        return results;
    }
}