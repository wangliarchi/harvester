package edu.olivet.harvester.common.model;

import com.alibaba.fastjson.JSON;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/21/2017 3:35 PM
 */
@Data
@Singleton
public class BuyerAccountSettingUtils {
    private List<BuyerAccountSetting> accountSettings = new ArrayList<>();

    private static BuyerAccountSettingUtils instance;

    private static final String SETTINGS_FILE_PATH = Directory.Customize.path() + "/buyer-accounts.json";
    private static final String TEST_SETTINGS_FILE_PATH = "src/test/resources/conf/buyer-accounts.json";


    private BuyerAccountSettingUtils() {

        File file = new File(getConfigPath());
        if (file.exists() && file.isFile()) {
            accountSettings = JSON.parseArray(Tools.readFileToString(file), BuyerAccountSetting.class);
        } else {
            try {
                loadFromSetting(Settings.load());
            } catch (Exception e) {
                //
            }

        }

        accountSettings.removeIf(it -> it.getBuyerAccount() == null);
        accountSettings.sort(Comparator.comparing(it -> it.getBuyerAccount().getEmail()));

    }

    public static BuyerAccountSettingUtils load(Settings settings) {
        instance = new BuyerAccountSettingUtils();
        instance.loadFromSetting(settings);
        return instance;
    }

    public static BuyerAccountSettingUtils load() {

        if (instance == null) {
            instance = new BuyerAccountSettingUtils();
        }

        return instance;
    }

    private void loadFromSetting(Settings settings) {
        for (Settings.Configuration config : settings.getConfigs()) {
            if (config.getBuyer() != null && StringUtils.isNotBlank(config.getBuyer().getEmail())) {
                migrateFromSetting(config.getBuyer(), config.getCountry(), OrderEnums.OrderItemType.BOOK, false);
            }

            if (config.getPrimeBuyer() != null && StringUtils.isNotBlank(config.getPrimeBuyer().getEmail())) {
                migrateFromSetting(config.getPrimeBuyer(), config.getCountry(), OrderEnums.OrderItemType.BOOK, true);
            }

            if (config.getProdBuyer() != null && StringUtils.isNotBlank(config.getProdBuyer().getEmail())) {
                migrateFromSetting(config.getProdBuyer(), config.getCountry(), OrderEnums.OrderItemType.PRODUCT, false);
            }

            if (config.getProdPrimeBuyer() != null && StringUtils.isNotBlank(config.getProdPrimeBuyer().getEmail())) {
                migrateFromSetting(config.getProdPrimeBuyer(), config.getCountry(), OrderEnums.OrderItemType.PRODUCT, true);
            }

        }
    }

    public BuyerAccountSetting getByEmail(String email) {
        for (BuyerAccountSetting accountSetting : accountSettings) {
            if (accountSetting.getBuyerAccount().getEmail().equalsIgnoreCase(email)) {
                return accountSetting;
            }
        }
        return null;
    }

    public List<Account> getAccounts(Country country) {
        return accountSettings.stream().filter(it ->
                StringUtils.equalsAnyIgnoreCase(it.getCountryName(), country.name(), "all"))
                .map(BuyerAccountSetting::getBuyerAccount)
                .collect(Collectors.toList());
    }

    public List<Account> getAccounts(Country country, OrderEnums.OrderItemType type, boolean primeBuyer) {

        return accountSettings.stream().filter(it ->
                StringUtils.equalsAnyIgnoreCase(it.getCountryName(), country.name(), "all") &&
                        StringUtils.equalsAnyIgnoreCase(it.getType(), type.name(), "both") &&
                        StringUtils.equalsAnyIgnoreCase(it.getPrimeBuyer(), primeBuyer ? "Prime" : "Non-Prime", "Both"))
                .map(BuyerAccountSetting::getBuyerAccount)
                .collect(Collectors.toList());
    }

    private void migrateFromSetting(Account buyer, Country country, OrderEnums.OrderItemType type, boolean primeBuyer) {
        BuyerAccountSetting accountSetting = getByEmail(buyer.getEmail());
        if (accountSetting == null) {
            accountSetting = new BuyerAccountSetting();
            accountSetting.setBuyerAccount(buyer);
            accountSetting.setCountryName(country.name());
            accountSetting.setPrimeBuyer(primeBuyer ? "Prime" : "Non-Prime");
            accountSetting.setType(type.name());
            accountSettings.add(accountSetting);
        } else {
            if (!accountSetting.getCountryName().equalsIgnoreCase(country.name())) {
                accountSetting.setCountryName("All");
            }

            if (!accountSetting.getType().equalsIgnoreCase(type.name())) {
                accountSetting.setType("Both");
            }
            boolean currentPrime = accountSetting.getPrimeBuyer().equalsIgnoreCase("Prime");
            if (currentPrime != primeBuyer) {
                accountSetting.setPrimeBuyer("Both");
            }
        }
    }

    public void save() {
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this.accountSettings, true));
    }

    public void save(BuyerAccountSetting buyerAccountSetting) {
        ListIterator<BuyerAccountSetting> iterator = accountSettings.listIterator();
        boolean existed = false;
        while (iterator.hasNext()) {
            BuyerAccountSetting setting = iterator.next();
            if (setting.getBuyerAccount().getEmail().equalsIgnoreCase(buyerAccountSetting.getBuyerAccount().getEmail())) {
                //Replace element
                iterator.set(buyerAccountSetting);
                existed = true;
                break;
            }
        }

        if (!existed) {
            accountSettings.add(buyerAccountSetting);
        }
        File file = new File(getConfigPath());
        Tools.writeStringToFile(file, JSON.toJSONString(this.accountSettings, true));
    }

    private static String getConfigPath() {
        if (Harvester.debugFlag) {
            return TEST_SETTINGS_FILE_PATH;
        }

        return SETTINGS_FILE_PATH;
    }
}
