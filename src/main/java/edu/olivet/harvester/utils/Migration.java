package edu.olivet.harvester.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.fulfill.utils.CreditCardUtils;
import edu.olivet.harvester.common.model.BuyerAccountSettingUtils;
import edu.olivet.harvester.common.model.CreditCard;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrate configuration from OrderMan
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/29/17 7:49 PM
 */
public class Migration {

    private static final Logger LOGGER = LoggerFactory.getLogger(Migration.class);

    @Setter
    @Getter
    private static boolean useMigration = false;

    public static boolean hasMigrationFile() {
        String settingFilePath = Directory.Customize.path() + File.separator + "accounts.js";
        File orderManConfigFile = new File(settingFilePath);
        if (orderManConfigFile.exists()) {
            return true;
        }

        orderManConfigFile = new File("C:\\OrderMan\\customize\\accounts.js");
        return orderManConfigFile.exists();

    }

    public static Settings loadSettings() {

        String settingFilePath = Directory.Customize.path() + File.separator + "accounts.js";
        File orderManConfigFile = new File(settingFilePath);
        if (!orderManConfigFile.exists()) {
            orderManConfigFile = new File("C:\\OrderMan\\customize\\accounts.js");
        }

        Settings settings = loadFromOrderManConfigFile(orderManConfigFile);
        BuyerAccountSettingUtils.load(settings).save();

        return settings;

    }

    /**
     * migrate Orderman account configuration to Harvester.
     */
    static Settings loadFromOrderManConfigFile(File file) {
        if (file.exists() && file.isFile()) {
            JSONObject setting = JSON.parseObject(Tools.readFileToString(file));
            final JSONArray countries = setting.getJSONArray("countries");
            JSONObject signatures = setting.getJSONObject("signatures");
            JSONObject googledrivebooks = setting.getJSONObject("googledrivebooks");
            JSONObject googledriveproducts = setting.getJSONObject("googledriveproducts");
            JSONObject orderFinders = setting.getJSONObject("orderFinders");
            JSONObject sellerids = setting.getJSONObject("sellerids");
            JSONObject sellers = setting.getJSONObject("sellers");

            final String sid = setting.getString("id");


            Map<String, Account.AccountType> orderManAccountTypeMapping = new HashMap<>();
            orderManAccountTypeMapping.put("seller", Account.AccountType.Seller);
            orderManAccountTypeMapping.put("sellerEmail", Account.AccountType.Email);
            orderManAccountTypeMapping.put("primeBuyer", Account.AccountType.PrimeBuyer);
            orderManAccountTypeMapping.put("ptBuyer", Account.AccountType.Buyer);
            orderManAccountTypeMapping.put("prodPrimeBuyer", Account.AccountType.PrimeBuyer);
            orderManAccountTypeMapping.put("prodPtBuyer", Account.AccountType.Buyer);
            orderManAccountTypeMapping.put("ebatesBuyer", Account.AccountType.EbatesBuyer);

            Map<String, String> orderManHarvesterAccountMapping = new HashMap<>();
            orderManHarvesterAccountMapping.put("ptBuyer", "buyer");
            orderManHarvesterAccountMapping.put("prodPtBuyer", "prodBuyer");


            List<Settings.Configuration> configs = new ArrayList<>(countries.size());

            for (int i = 0; i < countries.size(); i++) {
                Country country = Country.valueOf(countries.getString(i));

                Settings.Configuration cfg = new Settings.Configuration();
                cfg.setCountry(country);

                orderManAccountTypeMapping.forEach((String orderManKey, Account.AccountType accountType) -> {

                    String methodName;
                    methodName = orderManHarvesterAccountMapping.getOrDefault(orderManKey, orderManKey);

                    JSONObject accounts = setting.getJSONObject(orderManKey + "s");
                    JSONObject account = accounts.getJSONObject(country.name());

                    if (account != null && account.containsKey("email")) {
                        String accountString = String.format("%s/%s", account.getString("email"), account.getString("password"));
                        try {
                            Method method = cfg.getClass().getDeclaredMethod("set" + StringUtils.capitalize(methodName), Account.class);
                            method.invoke(cfg, new Account(accountString, accountType));
                        } catch (NoSuchMethodException e) {
                            LOGGER.error("No method found {} - {}", methodName, e);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            LOGGER.error("{} - {}", methodName, e);
                        }

                    }
                });

                try {
                    cfg.setBookDataSourceUrl(AppScript.getSpreadId(googledrivebooks.getString(country.name()).trim()));
                } catch (Exception e) {
                    //
                }

                try {
                    cfg.setProductDataSourceUrl(AppScript.getSpreadId(googledriveproducts.getString(country.name()).trim()));
                } catch (Exception e) {
                    //
                }

                try {
                    cfg.setSignature(signatures.getString(country.name()).trim());
                } catch (Exception e) {
                    //
                }

                try {
                    cfg.setUserCode(orderFinders.getString(country.name()).trim());
                } catch (Exception e) {
                    //
                }

                String selleridsString = sellerids.getString(country.name()).trim();
                if (selleridsString.isEmpty()) {
                    //check seller email, if accounts share the same email, they should share same mws credential.
                    //seller email must be there
                    String currentSellerEmail = sellers.getJSONObject(country.name()).getString("email");
                    for (Object o : countries) {
                        if (!o.equals(country.name()) && currentSellerEmail.equals(sellers.getJSONObject(o.toString()).getString("email")) && StringUtils.isNotEmpty(sellerids.getString(o.toString()))) {
                            selleridsString = sellerids.getString(o.toString()).trim();
                        }
                    }

                }

                //"A3BEPQLI451F6I/tATVPDKIKX0DER/tAKIAI73MGXM4FSWB6TIQ/tW5oIE4cbQ1MTG3YO5h4JU93J7FGjDX1/OLDjADBc/tMike Pro/t1.0/tnull"
                String[] mwsCredentialParts = selleridsString.split("/t");
                if (mwsCredentialParts.length > 3) {

                    String sellerId = mwsCredentialParts[0].trim();
                    String accessKey = mwsCredentialParts[2].trim();

                    String secretKey;
                    if (mwsCredentialParts.length == 7) {
                        secretKey = mwsCredentialParts[3].trim();
                    } else {
                        secretKey = mwsCredentialParts[4].trim() + mwsCredentialParts[3].trim();
                    }

                    cfg.setMwsCredential(new MarketWebServiceIdentity(sellerId, accessKey, secretKey, country.marketPlaceId()));
                    cfg.setStoreName(mwsCredentialParts[mwsCredentialParts.length - 7 + 4].trim());
                }

                configs.add(cfg);
            }

            return new Settings(sid, configs);

        } else {
            throw new IllegalStateException("Cannot find expected configuration file " + file.getAbsolutePath());
        }
    }

    public static void migrateCreditCardSettings() {
        File file = new File(CreditCardUtils.CC_CONFIG_FILE_PATH);
        if (file.exists()) {
            return;
        }

        String settingFilePath = Directory.Customize.path() + File.separator + "creditcards.js";
        File orderManConfigFile = new File(settingFilePath);
        if (!orderManConfigFile.exists()) {
            orderManConfigFile = new File("C:\\OrderMan\\customize\\creditcards.js");
        }

        if (!orderManConfigFile.exists()) {
            LOGGER.error("No credit card config file found.");
            return;
        }

        JSONObject creditcards = JSON.parseObject(Tools.readFileToString(orderManConfigFile));
        List<CreditCard> creditCards = new ArrayList<>();

        creditcards.forEach((k, v) -> {
            try {
                CreditCard card = new CreditCard();
                String[] parts = StringUtils.split(v.toString(), ",");
                card.setAccountEmail(k.trim());
                card.setCardNo(parts[0].trim());
                card.setCvv(parts[1].trim());
                creditCards.add(card);
            } catch (Exception e) {
                LOGGER.error("Failed to migrate credit card info,", e);
            }
        });

        CreditCardUtils.saveToFile(creditCards);

    }

    public static void main(String[] args) {
        Migration.migrateCreditCardSettings();
        //Migration.loadSettings();
    }

}
