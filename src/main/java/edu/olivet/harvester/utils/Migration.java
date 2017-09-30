package edu.olivet.harvester.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrate configuration from OrderMan
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/29/17 7:49 PM
 */
public class Migration {

    public static Settings loadSettings() {

        String settingFilePath = Directory.Customize.path() + File.separator + "accounts.js";
        File orderManConfigFile = new File(settingFilePath);
        if (!orderManConfigFile.exists()) {
            orderManConfigFile = new File("C:\\OrderMan\\customize\\accounts.js");
        }

        return loadFromOrderManConfigFile(orderManConfigFile);


    }

    static Settings loadFromOrderManConfigFile(File file) {
        if (file.exists() && file.isFile()) {
            JSONObject setting = JSON.parseObject(Tools.readFileToString(file));
            JSONArray countries = setting.getJSONArray("countries");
            JSONObject sellers = setting.getJSONObject("sellers");
            JSONObject sellerEmails = setting.getJSONObject("sellerEmails");
            JSONObject signatures = setting.getJSONObject("signatures");
            JSONObject googledrivebooks = setting.getJSONObject("googledrivebooks");
            JSONObject googledriveproducts = setting.getJSONObject("googledriveproducts");
            JSONObject primeBuyers = setting.getJSONObject("primeBuyers");
            JSONObject ptBuyers = setting.getJSONObject("ptBuyers");
            JSONObject prodPrimeBuyers = setting.getJSONObject("prodPrimeBuyers");
            JSONObject prodPtBuyers = setting.getJSONObject("prodPtBuyers");
            JSONObject ebatesBuyers = setting.getJSONObject("ebatesBuyers");
            JSONObject orderFinders = setting.getJSONObject("orderFinders");
            JSONObject sellerids = setting.getJSONObject("sellerids");
            String sid = setting.getString("id");


            List<Settings.Configuration> configs = new ArrayList<>(countries.size());


            for (int i = 0; i < countries.size(); i++) {
                Settings.Configuration cfg = new Settings.Configuration();

                Country country = Country.fromCode(countries.getString(i));
                cfg.setCountry(country);


                String seller = String.format("%s/%s", sellers.getJSONObject(country.code()).getString("email")
                        , sellers.getJSONObject(country.code()).getString("password"));
                cfg.setSeller(new Account(seller, Account.AccountType.Seller));

                String sellerEmail = String.format("%s/%s", sellerEmails.getJSONObject(country.code()).getString("email")
                        , sellerEmails.getJSONObject(country.code()).getString("password"));
                cfg.setSellerEmail(new Account(sellerEmail, Account.AccountType.Seller));


                String primeBuyer = String.format("%s/%s", primeBuyers.getJSONObject(country.code()).getString("email")
                        , primeBuyers.getJSONObject(country.code()).getString("password"));
                cfg.setPrimeBuyer(new Account(primeBuyer, Account.AccountType.PrimeBuyer));

                String ptBuyer = String.format("%s/%s", ptBuyers.getJSONObject(country.code()).getString("email")
                        , ptBuyers.getJSONObject(country.code()).getString("password"));
                cfg.setBuyer(new Account(ptBuyer, Account.AccountType.Buyer));


                String prodPrimeBuyer = String.format("%s/%s", prodPrimeBuyers.getJSONObject(country.code()).getString("email")
                        , prodPrimeBuyers.getJSONObject(country.code()).getString("password"));
                cfg.setProdPrimeBuyer(new Account(prodPrimeBuyer, Account.AccountType.PrimeBuyer));

                String prodPtBuyer = String.format("%s/%s", prodPtBuyers.getJSONObject(country.code()).getString("email")
                        , prodPtBuyers.getJSONObject(country.code()).getString("password"));
                cfg.setProdBuyer(new Account(prodPtBuyer, Account.AccountType.Buyer));


                String ebatesBuyer = String.format("%s/%s", ebatesBuyers.getJSONObject(country.code()).getString("email")
                        , ebatesBuyers.getJSONObject(country.code()).getString("password"));
                cfg.setEbatesBuyer(new Account(ebatesBuyer, Account.AccountType.Buyer));


                cfg.setBookDataSourceUrl(AppScript.getSpreadId(googledrivebooks.getString(country.code()).trim()));
                cfg.setProductDataSourceUrl(AppScript.getSpreadId(googledriveproducts.getString(country.code()).trim()));

                cfg.setSignature(signatures.getString(country.code()).trim());
                cfg.setUserCode(orderFinders.getString(country.code()).trim());

                String selleridsString = sellerids.getString(country.code()).trim();
                if (selleridsString.isEmpty()) {
                    if (country == Country.CA || country == Country.MX) {
                        selleridsString = sellerids.getString(Country.US.code()).trim();
                    }
                }

                String[] mwsCredentialParts = selleridsString.split("/t");
                if (mwsCredentialParts.length > 3) {
                    String sellerId = mwsCredentialParts[0].trim();
                    String accessKey = mwsCredentialParts[2].trim();
                    String secretKey = mwsCredentialParts[3].trim();

                    cfg.setMwsCredential(new MarketWebServiceIdentity(sellerId, accessKey, secretKey, country.marketPlaceId()));

                    cfg.setStoreName(mwsCredentialParts[4].trim());
                }

                configs.add(cfg);
            }

            return new Settings(sid, configs);

        } else {
            throw new IllegalStateException("Cannot find expected configuration file " + file.getAbsolutePath());
        }
    }

}
