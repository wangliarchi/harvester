package edu.olivet.harvester.service;

import com.ECS.client.jax.*;
import com.github.rchukh.amazon.paapi.AWSProps;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/13/17 10:08 AM
 */
@Singleton
public class AmazonProductApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonProductApi.class);

    private static AmazonProductApi instance = null;
    @SuppressWarnings("FieldCanBeLocal") private AWSECommerceService commerceService;
    private AWSECommerceServicePortType portUS;

    private static final int MAX_ASIN_COUNT_PER_REQUEST = 10;


    public static AmazonProductApi getInstance() {
        if (instance == null) {
            instance = new AmazonProductApi();
        }

        return instance;
    }

    private AmazonProductApi() {
        commerceService = new AWSECommerceService();
        portUS = commerceService.getAWSECommerceServicePortUS();
    }

    public HashMap<String, Item> itemLookup(List<String> asins) {
        //asins can only up to ten
        HashMap<String, Item> result = new HashMap<>();
        int index = 0, errorCount = 0;
        List<List<String>> lists = Lists.partition(asins, MAX_ASIN_COUNT_PER_REQUEST);


        for (List<String> list : lists) {
            index++;
            try {
                long start = System.currentTimeMillis();
                ItemLookupRequest itemLookupRequest = new ItemLookupRequest();
                ItemLookup itemLookup = new ItemLookup();
                itemLookup.setAWSAccessKeyId(AWSProps.INSTANCE.getAccessKeyId());
                itemLookup.setAssociateTag(AWSProps.INSTANCE.getDefaultAssociateTag());
                itemLookupRequest.getItemId().addAll(list);
                itemLookup.getRequest().add(itemLookupRequest);
                ItemLookupResponse itemLookupResponse = portUS.itemLookup(itemLookup);
                List<Item> items = itemLookupResponse.getItems().get(0).getItem();
                items.forEach(item -> result.put(item.getASIN(), item));
                LOGGER.info("Read {} items in {}.", list.size(), Strings.formatElapsedTime(start));
                WaitTime.Shortest.execute();
                //break;
            } catch (Exception e) {
                LOGGER.warn("Failed to request in group {}: {}", index, Strings.getExceptionMsg(e));
            }

        }
        return result;
    }
}
