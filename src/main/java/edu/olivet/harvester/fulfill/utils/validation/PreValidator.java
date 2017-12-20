package edu.olivet.harvester.fulfill.utils.validation;

import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.fulfill.service.CompareItemNameWorker;
import edu.olivet.harvester.fulfill.utils.ISBNUtils;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.utils.common.ThreadHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:57 AM
 */
public class PreValidator {


    private static final Logger LOGGER = LoggerFactory.getLogger(PreValidator.class);
    private static final int COMPARE_JOB_NUMBER = 2;


    public static List<ItemCompareResult> compareItemNames4Orders(List<Order> orders) {
        long start = System.currentTimeMillis();
        final List<ItemCompareResult> results = new ArrayList<>(orders.size());
        List<Order> ordersToCheck = new ArrayList<>();

        //getTitleFromCache
        ISBNUtils.initCache();
        ItemValidator itemValidator = ApplicationContext.getBean(ItemValidator.class);
        orders.forEach(order -> {
            String title = ISBNUtils.getTitleFromCache(OrderCountryUtils.getFulfillmentCountry(order), order.isbn);
            if (StringUtils.isNotBlank(title)) {
                ItemValidator.ValidateReport report = itemValidator.validateItemName(title, order.item_name);
                results.add(new ItemCompareResult(order, title, report.pass, false, report.toString()));
            } else {
                ordersToCheck.add(order);
            }
        });


        int job_number = 1;
        if (ordersToCheck.size() > 10) {
            job_number = COMPARE_JOB_NUMBER;
        }


        List<List<Order>> list = ThreadHelper.assign(ordersToCheck, job_number);
        List<CompareItemNameWorker> jobs = new ArrayList<>(job_number);

        for (List<Order> assignedOrders : list) {
            if (CollectionUtils.isEmpty(assignedOrders)) {
                continue;
            }
            jobs.add(new CompareItemNameWorker(assignedOrders));
        }

        for (CompareItemNameWorker job : jobs) {
            job.execute();
        }

        for (CompareItemNameWorker job : jobs) {
            try {
                List<ItemCompareResult> singleJobResult = job.get();
                if (CollectionUtils.isNotEmpty(singleJobResult)) {
                    results.addAll(singleJobResult);
                }
            } catch (Exception e) {
                LOGGER.warn("多线程比较书名过程中出现异常:{}", e);
            }
        }


        if (results.size() > 0) {
            Collections.sort(results);
        }
        LOGGER.debug("{}条订单书名比较完成，耗时:{}", orders.size(), Strings.formatElapsedTime(start));
        return results;


    }


}
