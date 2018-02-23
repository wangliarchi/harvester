package edu.olivet.harvester.feeds.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 1:36 PM
 */
public class AsinAppScript extends AppScript {

    protected static final String APP_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwmiA0NbnIWn_8vs0NN1VSxrGUNd9VRNsMPKGfv61EFUM6mtP6n/exec";

    @Override
    protected String getBaseUrl() {
        return APP_SCRIPT_URL;
    }

    public List<String> getASINS(Map<String, String> params) {
        params.put(PARAM_METHOD, "READASINS");
        String result = this.processResult(this.get(params));
        List<String> asins = Lists.newArrayList(StringUtils.split(result, ","));

        asins.removeIf(asin -> !Regex.ASIN.isMatched(asin));
        //?
        asins = ImmutableSet.copyOf(asins).asList();
        return asins;
    }

    public List<String> getAsinInventoryLoader() {
        Map<String, String> params = new HashMap<>();
        params.put("flag", "X");
        return getASINS(params);
    }

    public List<String> getAsinInventoryLoaderSync() {
        Map<String, String> params = new HashMap<>();
        params.put("flag", "XS");
        return getASINS(params);
    }

    public List<String> getAsinEmptyType() {
        Map<String, String> params = new HashMap<>();
        params.put("flag", "XS");
        return getASINS(params);
    }

    public String writeASINS(String context, String asins, Map<String, String> params) {
        if (StringUtils.isBlank(context) || StringUtils.isBlank(asins)) {
            throw new IllegalArgumentException(UIText.message("error.asinctx.empty"));
        }
        params.put("ctx", StringUtils.defaultString(context));
        params.put("asins", StringUtils.defaultString(asins));
        params.put(PARAM_METHOD, "WRITEASINS");
        return this.exec(params);
    }

    public String writeASINSInventoryLoader(String context, String asins) {
        Map<String, String> params = new HashMap<>();
        params.put("flag", "X");
        return writeASINS(context, asins, params);
    }

    public String writeASINSInventoryLoaderSync(String context, String asins) {
        Map<String, String> params = new HashMap<>();
        params.put("flag", "XS");
        return writeASINS(context, asins, params);
    }

    public String writeASINSEmpty(String context, String asins) {
        Map<String, String> params = new HashMap<>();
        params.put("flag", StringUtils.EMPTY);
        return writeASINS(context, asins, params);
    }
}
