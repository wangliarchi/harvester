package edu.olivet.harvester.service;

import com.amazonservices.mws.products.model.Product;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Profile;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.service.mws.ProductAttributesHelper;
import edu.olivet.harvester.service.mws.ProductClient;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * helper class to determine order item type, either book or product
 */
public class OrderItemTypeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderItemTypeHelper.class);

    @Inject
    @Setter
    private ProductClient productClient;

    @Profile
    public OrderEnums.OrderItemType getItemType(Order order) {

        OrderEnums.OrderItemType type;

        //To save time, we will try sku pattern first.

        try {

            type = this.getItemTypeBySku(order);

            //LOGGER.info("Order item type for {} found as {} from SKU pattern. ASIN {}, SKU {}",
            //order.order_id,
            //type.name(), order.isbn, order.sku);

            return type;

        } catch (Exception e) {
            //LOGGER.warn("No product group info found by sku pattern for {}, ASIN {}, SKU {} - {}", order.order_id, order.isbn, order.sku, e);
        }

        /*
         Disable now. API quote has been used up by repricing programs.
         try {

         type = this.getItemTypeByMWSAPI(order);
         LOGGER.info("Order item type for {} found as {} from MWS product API. ASIN {}, SKU {}",
         order.order_id,
         type.name(), order.isbn, order.sku);

         return type;

         } catch (Exception e) {
         LOGGER.warn(
         "No product group info found by product api for {}, ASIN {}, SKU {} - {}, will set default to BOOK", order.order_id, order.isbn, order.sku, e
         );
         }
         */

        //TODO: default to BOOK?

        return OrderEnums.OrderItemType.BOOK;

    }

    public OrderEnums.OrderItemType getItemTypeBySku(Order order) {

        String sku = order.getSku();

        if (sku.isEmpty()) {
            throw new BusinessException("No valid SKU found for order " + order.order_id);
        }

        String[] productKeywords = {"ART", "AUTO", "pro", "jewel", "shoe", "cloth", "watch", "BABY", "BEAU", "ELEC", "FOOD", "HARDWARE", "HEAL", "HOME", "MEASURE", "OFFICE", "PET", "SAFETY", "SPOR", "TOOL", "TOY", "access", "guowai", "bady", "kit", "out", "uban"};

        if (Strings.containsAnyIgnoreCase(sku, productKeywords)) {
            return OrderEnums.OrderItemType.PRODUCT;
        }


        String[] bookKeywords = {"book", "bk", "dvd", "cd"};

        for (String keyword : bookKeywords) {
            if (sku.toLowerCase().contains(keyword.toLowerCase())) {
                return OrderEnums.OrderItemType.BOOK;
            }
        }

        throw new BusinessException("Order item type for {} not found from SKU pattern. ASIN " + order.isbn + ", SKU " + sku);

    }

    public OrderEnums.OrderItemType getItemTypeByMWSAPI(Order order) {

        String asin = "";

        if (!order.getSku_address().isEmpty()) {
            asin = RegexUtils.getMatched(order.getSku_address(), RegexUtils.Regex.ASIN);

        }
        if (asin.isEmpty() && !order.getIsbn().isEmpty()) {
            asin = order.getIsbn();
        }


        if (asin.isEmpty()) {
            //LOGGER.error("No valid ASIN found for order {} on row {}",order.order_id,order.getRow());
            throw new BusinessException("No valid ASIN found for order " + order.order_id);
        }

        String productGroup;
        try {
            Product product = productClient.getProductByASIN(Country.fromSalesChanel(order.getSales_chanel()), asin);
            productGroup = ProductAttributesHelper.getProductGroup(product);

            if (productGroup.isEmpty()) {
                throw new BusinessException("No product group info found from mws product api.");
            }

        } catch (Exception e) {
            throw new BusinessException("No product group info found from mws product api.");
        }


        String[] bookTypes = {"book", "dvd", "music"};

        if (Arrays.asList(bookTypes).contains(productGroup.toLowerCase())) {
            return OrderEnums.OrderItemType.BOOK;
        }

        return OrderEnums.OrderItemType.PRODUCT;
    }
}
