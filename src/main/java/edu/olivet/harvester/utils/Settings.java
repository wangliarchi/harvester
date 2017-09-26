package edu.olivet.harvester.utils;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.ui.Harvester;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of Harvester, usually consisting of seller account, buyer accounts and etc.
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 9:28 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Settings {

    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    public static Settings load() {
        File file = new File(Harvester.CONFIG_FILE_PATH);
        if (file.exists() && file.isFile()) {
            return JSON.parseObject(Tools.readFileToString(file), Settings.class);
        } else {
            throw new IllegalStateException("Cannot find expected configuration file " + Harvester.CONFIG_FILE_PATH);
        }
    }

    private String sid;

    private List<Configuration> configs;

    public Configuration getConfigByCountry(Country country) {
        for(Configuration config : configs) {
            if (config.country.equals(country)) {
                return config;
            }
        }

        throw new BusinessException("Configuration for country "+country.name()+" is not found.");
    }


    /**
     * @return
     */
    public List<String> listAllSpreadsheets() {

        List<String> spreadIds = new ArrayList<>();

        for (Configuration config : configs) {
            if (!config.getBookDataSourceUrl().isEmpty()) {
                if (!spreadIds.contains(config.getBookDataSourceUrl())) {
                    spreadIds.add(config.getBookDataSourceUrl());
                }
            }
            if (!config.getProductDataSourceUrl().isEmpty()) {
                if (!spreadIds.contains(config.getProductDataSourceUrl())) {
                    spreadIds.add(config.getProductDataSourceUrl());
                }
            }
        }

        if (spreadIds.isEmpty()) {
            LOGGER.error("No valid sheet ids found when confirming shipment");
            throw new BusinessException("No googlesheet entered yet.");
        }

        return spreadIds;
    }

    /**
     * Configuration for a certain marketplace
     */
    @Data
    public static class Configuration {
        private Country country;

        private Account seller, sellerEmail;

        private Account primeBuyer, buyer;

        private Account prodPrimeBuyer, prodBuyer;

        private Account ebayBuyer;

        private String bookDataSourceUrl, productDataSourceUrl;

        private String storeName, signature;

        private String userCode;

        private MarketWebServiceIdentity mwsCredential;

        public List<String> validate() {
            List<String> list = new ArrayList<>();
            if (country == null) {
                list.add("Marketplace not provided");
            }

            if (seller == null || !seller.valid()) {
                list.add("Seller account not provided or invalid");
            }

            if (mwsCredential == null || !mwsCredential.valid()) {
                list.add("MWS API credential not provided or invalid");
            }

            if (sellerEmail == null || !sellerEmail.valid()) {
                list.add("Seller email not provided or invalid");
            }

            if (StringUtils.isAnyBlank(storeName, signature)) {
                list.add("Both store name and email signature should be provided for contacting customer via email");
            }

            if (StringUtils.isAllBlank(bookDataSourceUrl, productDataSourceUrl)) {
                list.add("Book and product order update spreadsheet url cannot be both empty");
            } else if (StringUtils.isNotBlank(bookDataSourceUrl)) {
                if (primeBuyer == null || !primeBuyer.valid() || buyer == null || !buyer.valid()) {
                    list.add("Both prime buyer and non-prime buyer should be provided since book order data source configured");
                }
            } else if (StringUtils.isNotBlank(productDataSourceUrl)) {
                if (prodPrimeBuyer == null || !prodPrimeBuyer.valid() || prodBuyer == null || !prodBuyer.valid()) {
                    list.add("Both prime buyer and non-prime buyer should be provided since product order data source configured");
                }
            }

            if (!this.consistent(primeBuyer, buyer)) {
                list.add("Book prime buyer and non-prime buyer validity inconsistent");
            }

            if (!this.consistent(prodPrimeBuyer, prodBuyer)) {
                list.add("Product prime buyer and non-prime buyer validity inconsistent");
            }

            if (StringUtils.isBlank(userCode)) {
                list.add("User code not provided");
            }

            if (StringUtils.isNotBlank(productDataSourceUrl) && (ebayBuyer == null || !ebayBuyer.valid())) {
                list.add("Ebay buyer account must be provided as product order fulfillment requires it");
            }

            return list;
        }

        /**
         * Check consistence of prime buyer and non-prime buyer: shall be both valid or both empty
         */
        private boolean consistent(Account primeBuyer, Account buyer) {
            boolean primeValid = primeBuyer != null && primeBuyer.valid();
            boolean valid = buyer != null && buyer.valid();
            return primeValid == valid;
        }


        /**
         * get configed spreadsheet id by type: book or product
         * @param type
         * @return
         */
        public String getSpreadId(OrderEnums.OrderItemType type) {

            String spreadId = this.getBookDataSourceUrl();

            if (type == OrderEnums.OrderItemType.PRODUCT) {
                spreadId = this.getProductDataSourceUrl();
            }

            return spreadId;
        }
    }

}
