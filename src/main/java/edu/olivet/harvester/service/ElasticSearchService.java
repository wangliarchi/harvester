package edu.olivet.harvester.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.WaitTime;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/28/2017 11:36 AM
 */
public class ElasticSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchService.class);
    private static final String ELASTIC_SEARCH_ADDRESS = "http://35.188.127.209";
    private static final String PRODUCT_INDEX = "product";
    private static final String LISTING_MAPPING_INDEX = "listing-mapping";
    private static final int MAX_ASIN_COUNT_PER_REQUEST = 15;

    public String searchISBN(String asin) {
        Map<String, Object> params = new HashMap<>();
        params.put("q", "asin:" + "\"" + StringUtils.strip(asin) + "\"");
        params.put("pretty", "true");
        params.put("size", 1);

        String json = request(LISTING_MAPPING_INDEX, params);
        JSONObject response = JSON.parseObject(json);
        int total = response.getJSONObject("hits").getInteger("total");
        if (total > 0) {
            JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");
            if (hits.size() > 0) {
                return ((JSONObject) hits.get(0)).getJSONObject("_source").getString("isbn");
            }

        }

        return null;
    }

    public Map<String, String> searchISBNs(List<String> asins) {
        List<List<String>> lists = Lists.partition(asins, MAX_ASIN_COUNT_PER_REQUEST);
        Map<String, String> results = new HashMap<>();

        for (List<String> list : lists) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("q", "asin:" + StringUtils.join(list.stream().map(it -> "\"" + StringUtils.strip(it) + "\"")
                        .collect(Collectors.toList()), ","));
                params.put("pretty", "true");
                params.put("size", MAX_ASIN_COUNT_PER_REQUEST);

                String json = request(LISTING_MAPPING_INDEX, params);
                JSONObject response = JSON.parseObject(json);
                int total = response.getJSONObject("hits").getInteger("total");
                if (total > 0) {
                    JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");
                    for (Object hit : hits) {
                        String isbn = ((JSONObject) hit).getJSONObject("_source").getString("isbn");
                        String asin = ((JSONObject) hit).getJSONObject("_source").getString("asin");
                        results.put(asin, isbn);
                    }
                }

                WaitTime.Shortest.execute();
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }

        return results;
    }

    public Map<String, String> searchTitle(List<String> asins) {

        List<List<String>> lists = Lists.partition(asins, MAX_ASIN_COUNT_PER_REQUEST);
        Map<String, String> results = new HashMap<>();

        for (List<String> list : lists) {
            Map<String, Object> params = new HashMap<>();
            params.put("q", "asin:" + StringUtils.join(list.stream().map(it -> "\"" + StringUtils.strip(it) + "\"")
                    .collect(Collectors.toList()), ","));
            params.put("pretty", "true");
            params.put("size", MAX_ASIN_COUNT_PER_REQUEST);


            String json = request(PRODUCT_INDEX, params);
            JSONObject response = JSON.parseObject(json);
            int total = response.getJSONObject("hits").getInteger("total");
            if (total > 0) {
                JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");
                for (Object hit : hits) {
                    String title = ((JSONObject) hit).getJSONObject("_source").getString("title");
                    String asin = ((JSONObject) hit).getJSONObject("_source").getString("asin");
                    results.put(asin, title);
                }
            }
        }

        return results;

    }

    public void addProductIndex(String asin, String title, String brand, Country country) {
        String id = asin + "-" + country.name();
        if (StringUtils.isBlank(title)) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("asin", asin);
        params.put("title", title);
        if (StringUtils.isNotBlank(brand)) {
            params.put("brand", brand);
        }
        addIndex(PRODUCT_INDEX, id, params);
    }

    @Repeat
    public String request(String index, Map<String, Object> params) {
        String params4Url = params2Url(params);
        String url = ELASTIC_SEARCH_ADDRESS + "/" + index + "/_search" + params4Url;
        try {
            return Jsoup.connect(url).timeout(WaitTime.Longer.valInMS())
                    .ignoreContentType(true).execute().body();
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    public void addIndex(String index, String id, Map<String, Object> params) {
        String url = ELASTIC_SEARCH_ADDRESS + "/" + index + "/" + index;
        if (StringUtils.isNotBlank(id)) {
            url = url + "/" + id;
        }
        String json = JSON.toJSONString(params);
        try {
            Jsoup.connect(url).requestBody(json).header("Content-Type", "application/json")
                    .timeout(WaitTime.Longer.valInMS())
                    .ignoreContentType(true).post();
        } catch (Exception e) {
            throw Lang.wrapThrow(e);
        }
    }


    private String params2Url(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            sb.append("?");
            int i = 0;
            for (Entry<String, Object> entry : params.entrySet()) {
                if (i++ > 0) {
                    sb.append("&");
                }
                sb.append(entry.getKey()).append("=");
                String value = entry.getValue().toString();
                sb.append(value);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        ElasticSearchService elasticSearchService = ApplicationContext.getBean(ElasticSearchService.class);
        List<String> asins = Lists.newArrayList("1423221656", "158901698X", "1586484230");
        Map<String, String> results = elasticSearchService.searchTitle(asins);
        System.out.println(results);
        //elasticSearchService.addProductIndex("B00006IJSG", "Bruder - MAN Garbage Truck Orange - 3+", "Bruder Toys", Country.US);
    }
}
