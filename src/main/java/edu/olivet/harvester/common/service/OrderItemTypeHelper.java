package edu.olivet.harvester.common.service;

import com.amazonservices.mws.products.model.Product;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Profile;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.*;
import edu.olivet.harvester.common.service.mws.ProductAttributesHelper;
import edu.olivet.harvester.common.service.mws.ProductClient;
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

    public OrderItemType getItemType(Order order) {
        //To save time, we will try sku pattern first.
        try {
            OrderItemType type = getItemTypeBySku(order);
            return type;
        } catch (Exception e) {
            //LOGGER.warn("No product group info found by sku pattern for {}, ASIN {}, SKU {} - {}",
            // order.order_id, order.isbn, order.sku, e);
        }

        return OrderItemType.BOOK;
    }

    public static OrderItemType getItemTypeBySku(Order order) {
        String sku = order.getSku();

        if (sku.isEmpty()) {
            throw new BusinessException("No valid SKU found for order " + order.order_id);
        }

        String[] productKeywords = {"ART", "AUTO", "pro", "jewel", "shoe", "cloth", "watch", "BABY",
                "BEAU", "ELEC", "FOOD", "HARDWARE", "HEAL", "HOME", "MEASURE", "OFFICE", "PET",
                "SAFETY", "SPOR", "TOOL", "TOY", "access", "guowai", "bady", "kit", "out", "uban"};

        if (Strings.containsAnyIgnoreCase(sku, productKeywords)) {
            return OrderItemType.PRODUCT;
        }

        String[] bookKeywords = {"book", "bk", "dvd", "cd"};

        for (String keyword : bookKeywords) {
            if (sku.toLowerCase().contains(keyword.toLowerCase())) {
                return OrderItemType.BOOK;
            }
        }

        throw new BusinessException("Order item type for {} not found from SKU pattern. ASIN " + order.isbn + ", SKU " + sku);
    }

    public OrderItemType getItemTypeByMWSAPI(Order order) {

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
            return OrderItemType.BOOK;
        }

        return OrderItemType.PRODUCT;
    }
}
