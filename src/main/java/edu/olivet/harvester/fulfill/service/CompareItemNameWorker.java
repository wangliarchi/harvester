package edu.olivet.harvester.fulfill.service;

import com.ECS.client.jax.Item;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.utils.validation.ItemValidator;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.service.AmazonProductApi;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

        List<String> asins = orders.stream().map(it -> it.isbn).collect(Collectors.toList());

        HashMap<String, Item> items = AmazonProductApi.getInstance().itemLookup(asins);

        for (Order order : orders) {
            if (!items.containsKey(order.isbn)) {
                continue;
            }
            String title = items.get(order.isbn).getItemAttributes().getTitle();
            if (StringUtils.isBlank(title)) {
                continue;
            }

            String itemName = order.item_name.trim();
            ItemValidator.ValidateReport report = itemValidator.validateItemName(title, itemName);
            results.add(new ItemCompareResult(order, title, report.pass, false, report.toString()));
        }

        LOGGER.debug("共计{}条订单，有效书名比较结果{}条", orders.size(), results.size());
        return results;
    }


}