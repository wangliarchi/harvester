package edu.olivet.harvester.hunt.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/13/17 4:15 PM
 */
public class ForbiddenSellerService extends AppScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForbiddenSellerService.class);
    private static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxJd7lVCPpKiezuF1-CbYXCw6zbY8UQbYHqMdSJZ1X3kDD4diBX/exec?method=getsellers";

    private static final Map<String, List<String>> REGION_FORBIDDEN_LIST_CACHE = new HashMap<>();

    public boolean isForbidden(Seller seller) {
        List<String> forbiddenSellers = REGION_FORBIDDEN_LIST_CACHE
                .computeIfAbsent(seller.getOfferListingCountry().name(), key -> load(seller.getOfferListingCountry()));

        return CollectionUtils.containsAny(forbiddenSellers,
                Lists.newArrayList(seller.getName().toLowerCase(), seller.getUuid().toLowerCase()));

    }

    private List<String> load(Country country) {
        List<String> forbiddenSellers = new ArrayList<>();
        File localFile = new File(Directory.Customize.path() + File.separator + Config.ForbiddenSellers.fileName());
        String json = null;

        //local cache for 2 hours
        if (localFile.exists()) {
            try {
                Path p = Paths.get(localFile.getAbsolutePath());
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                if (System.currentTimeMillis() - attr.lastModifiedTime().toMillis() < 2 * 60 * 60 * 1000) {
                    json = Tools.readFileToString(localFile);
                }
            } catch (Exception e) {
                //
            }
        }

        if (json == null) {
            json = processResult(get());
            Tools.writeStringToFile(localFile, json);
        }

        List<JSONObject> regions = JSON.parseArray(json, JSONObject.class);

        for (JSONObject object : regions) {
            String countryCode = object.get("country").toString();
            if (country.name().equalsIgnoreCase(countryCode) || (country.europe() && "eu".equalsIgnoreCase(countryCode))) {
                forbiddenSellers.addAll(object.getJSONArray("ids").stream().filter(it ->
                        StringUtils.isNotBlank(it.toString())).map(it -> it.toString().toLowerCase()).collect(Collectors.toList()));
                forbiddenSellers.addAll(object.getJSONArray("names").stream().filter(it ->
                        StringUtils.isNotBlank(it.toString())).map(it -> it.toString().toLowerCase()).collect(Collectors.toList()));
                break;
            }
        }

        return forbiddenSellers;
    }

    private String get() {
        try {
            return Jsoup.connect(APP_SCRIPT_URL).timeout(WaitTime.Longer.valInMS()).ignoreContentType(true).execute().body();
        } catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    public static void main(String[] args) {
        ForbiddenSellerService f = ApplicationContext.getBean(ForbiddenSellerService.class);
        f.load(Country.US);
    }
}
