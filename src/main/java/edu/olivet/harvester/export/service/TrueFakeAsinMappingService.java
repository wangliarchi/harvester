package edu.olivet.harvester.export.service;

import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.service.ElasticSearchService;
import edu.olivet.harvester.utils.common.HttpUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/19/17 8:00 PM
 */
public class TrueFakeAsinMappingService {
    private static final String SERVICE_URL = "http://35.166.131.3:8080/listing-mapping/api/mapping/%s?accessKey=7c2eeed11dab9a747e7517583e6ee857";

    @Repeat(expectedExceptions = BusinessException.class)
    public static String getISBN(String sku, String asin) {
        ElasticSearchService elasticSearchService = new ElasticSearchService();
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


}
