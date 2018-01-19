package edu.olivet.harvester.fulfill.service;

import com.ECS.client.jax.Item;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.utils.ISBNUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.fulfill.utils.validation.ItemValidator;
import edu.olivet.harvester.logger.ISBNLogger;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.service.AmazonProductApi;
import edu.olivet.harvester.common.service.ElasticSearchService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        ElasticSearchService elasticSearchService = ApplicationContext.getBean(ElasticSearchService.class);
        Thread.currentThread().setName("CompareItemName");

        List<ItemCompareResult> results = new ArrayList<>(orders.size());
        ItemValidator itemValidator = ApplicationContext.getBean(ItemValidator.class);

        List<String> asins = orders.stream().filter(it -> Regex.ASIN.isMatched(it.isbn)).map(it -> it.isbn).collect(Collectors.toList());

        //check from elasticsearch service first
        Map<String, String> asinTitles = elasticSearchService.searchTitle(asins);
        asins.removeIf(asinTitles::containsKey);

        HashMap<String, Item> items = AmazonProductApi.getInstance().itemLookup(asins);

        for (Order order : orders) {
            String title = "";
            if (asinTitles.containsKey(order.isbn)) {
                title = asinTitles.get(order.isbn);
            } else if (items.containsKey(order.isbn)) {
                title = items.get(order.isbn).getItemAttributes().getTitle();
                String brand = items.get(order.isbn).getItemAttributes().getBrand();
                elasticSearchService.addProductIndex(order.isbn, title, brand, Country.US);
            }

            if (StringUtils.isBlank(title)) {
                title = ISBNUtils.getTitle(OrderCountryUtils.getFulfillmentCountry(order), order.isbn);
                elasticSearchService.addProductIndex(order.isbn, title, "", Country.US);
            }

            if (StringUtils.isBlank(title)) {
                continue;
            }

            ISBNLogger.save(Country.US + Constants.HYPHEN + order.isbn + "=" + title);
            String itemName = order.item_name.trim();
            ItemValidator.ValidateReport report = itemValidator.validateItemName(title, itemName);
            results.add(new ItemCompareResult(order, title, report.pass, false, report.toString()));
        }

        LOGGER.debug("共计{}条订单，有效书名比较结果{}条", orders.size(), results.size());
        return results;
    }


}