package edu.olivet.harvester.export.service;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.export.model.AmazonOrder;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.common.service.ElasticSearchService;
import edu.olivet.harvester.utils.http.HttpUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 8:00 PM
 */
public class TrueFakeAsinMappingService {
    private static final String SERVICE_URL = "http://35.166.131.3:8080/listing-mapping/api/mapping/%s?accessKey=7c2eeed11dab9a747e7517583e6ee857";

    @Inject private ElasticSearchService elasticSearchService;

    public void getISBNs(List<AmazonOrder> orders) {
        List<String> asins = new ArrayList<>();

        //filter out zhendian
        orders.forEach(order -> {
            if (Strings.containsAnyIgnoreCase(order.getSku(), "ZD", "zendian")) {
                order.setIsbn(order.getAsin());
            } else {
                asins.add(order.getAsin());
            }
        });

        Map<String, String> listingMapping = elasticSearchService.searchISBNs(asins);

        orders.forEach(order -> {
            if (listingMapping.containsKey(order.getAsin())) {
                order.setIsbn(listingMapping.get(order.getAsin()));
            } else if (elasticSearchService.isTrueASIN(order.getAsin())) {
                order.setIsbn(order.getAsin());
            } else if (StringUtils.isBlank(order.getIsbn())) {
                try {
                    String isbn = HttpUtils.getText(String.format(SERVICE_URL, order.getAsin()));
                    if (!"No source ASIN/ISBN found.".equalsIgnoreCase(isbn)) {
                        order.setIsbn(isbn);
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
        });
    }

    public String getISBN(Order order) {
        return getISBN(order.sku, order.getASIN());
    }

    @Repeat(expectedExceptions = BusinessException.class)
    public String getISBN(String sku, String asin) {
        String isbn = null;
        try {
            isbn = elasticSearchService.searchISBN(asin);
        } catch (Exception e) {
            //wrong with elasticsearch server, send alert
            ErrorAlertService errorAlertService = ApplicationContext.getBean(ErrorAlertService.class);
            errorAlertService.sendMessage("Error with elasticsearch server!!", asin + "\n" + e.getMessage());
        }

        if (StringUtils.isBlank(isbn)) {
            try {
                isbn = HttpUtils.getText(String.format(SERVICE_URL, asin));
            } catch (Exception e) {
                //ignore
            }
        }

        if (RegexUtils.Regex.ASIN.isMatched(isbn)) {
            return isbn;
        } else if (Strings.containsAnyIgnoreCase(sku, "ZD", "zendian")) {
            return asin;
        } else {
            throw new BusinessException(String.format("Cannot find source ISBN for '%s' from query result '%s'", asin, isbn));
        }

    }

    public static void main(String[] args) {
        TrueFakeAsinMappingService trueFakeAsinMappingService = ApplicationContext.getBean(TrueFakeAsinMappingService.class);
        String isbn = trueFakeAsinMappingService.getISBN("XINCAJYLUsFeb12-2018-PYBEYE21864","B003YQW2V2");
        System.out.println(isbn);
    }

}
