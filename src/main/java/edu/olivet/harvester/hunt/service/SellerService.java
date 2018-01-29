package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Or;
import com.teamdev.jxbrowser.chromium.Browser;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.*;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.common.model.Money;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.hunt.model.Rating;
import edu.olivet.harvester.hunt.model.Rating.RatingType;
import edu.olivet.harvester.hunt.model.Seller;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerFullType;
import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import edu.olivet.harvester.hunt.utils.SellerHuntUtil;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.common.NumberUtils;
import edu.olivet.harvester.utils.http.HtmlFetcher;
import edu.olivet.harvester.utils.http.HtmlParser;
import edu.olivet.harvester.utils.order.PageUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:38 AM
 */
public class SellerService {
    private static final SellerHuntingLogger LOGGER = SellerHuntingLogger.getInstance();

    private Map<String, Boolean> wareHouseIdCache = new HashMap<>();
    /**
     * 程序找单时默认翻页数上限:{@value}
     */
    public static final int MAX_PAGE = 3;
    private static I18N I18N_AMAZON;

    @Inject Now now;
    @Inject SellerFilter sellerFilter;
    @Inject HtmlFetcher htmlFetcher;

    @Inject
    public void init() throws IOException {
        Map<String, String> wareHouseIds = Configs.load("wrsellerids.properties");
        for (Map.Entry<String, String> entry : wareHouseIds.entrySet()) {
            wareHouseIdCache.put(entry.getValue(), true);
        }
        I18N_AMAZON = new I18N("i18n/Amazon");
        htmlFetcher.setSilentMode(true);
    }

    public List<Seller> getSellersForOrder(Order order) {
        if (StringUtils.isBlank(order.isbn)) {
            throw new BusinessException("No ISBN found for order yet");
        }

        Map<Country, List<SellerFullType>> countriesToHunt = SellerHuntUtil.countriesToHunt(order);
        LOGGER.info(order, "Trying to find sellers for {} {} from {} {} - {}",
                order.isbn, order.original_condition,
                countriesToHunt.size(), countriesToHunt.size() == 1 ? "country" : "countries", countriesToHunt);

        List<Seller> sellers = new ArrayList<>();

        countriesToHunt.forEach((country, types) -> {
            List<Seller> sellersFromCountry = getSellersByCountry(country, order, types);
            LOGGER.info(order, String.format("found %d sellers from %s \n",
                    sellersFromCountry.size(), country.name()));
            sellers.addAll(sellersFromCountry);
        });

        return sellers;
    }

    public List<Seller> getSellersByCountry(Country country, Order order) {
        List<SellerFullType> allowedTypes = SellerHuntUtil.countriesToHunt(order).getOrDefault(country, null);
        if (CollectionUtils.isEmpty(allowedTypes)) {
            throw new BusinessException("Country " + country + " is not allowed for order " + order.order_number + " " + order.isbn);
        }
        return getSellersByCountry(country, order, allowedTypes);
    }

    public List<Seller> getSellersByCountry(Country country, Order order, List<SellerFullType> allowedTypes) {
        String isbn = Strings.fillMissingZero(order.isbn);
        String url = getOfferListingPageUrl(country, isbn, order.originalCondition());

        List<Seller> sellers = new ArrayList<>();
        for (int i = 0; i < MAX_PAGE; i++) {
            Document document;
            try {
                document = htmlFetcher.getDocument(url);
            } catch (Exception e) {
                //
                continue;
            }
            LOGGER.saveHtml(order, order.isbn + "-offer-listing-page-" + (i + 1), document.outerHtml());

            List<Seller> sellersByPage = parseSellers(document, country);

            LOGGER.info(order, "Found {} sellers on page {} on {}, url {} ", sellersByPage.size(), i + 1, country.baseUrl(), url);
            if (CollectionUtils.isEmpty(sellersByPage)) {
                break;
            }
            Seller lastSeller = sellersByPage.get(sellersByPage.size() - 1);


            //remove unqualified sellers
            sellersByPage.removeIf(seller -> !sellerFilter.isPreliminaryQualified(seller, order)
                    || !sellerFilter.typeAllowed(seller, order, allowedTypes)
            );

            //get seller ratings on seller profile page
            sellersByPage.forEach(seller -> this.getSellerRatings(seller, order));

            //remove unqualified sellers
            sellersByPage.removeIf(seller -> !sellerFilter.isQualified(seller, order));

            sellers.addAll(sellersByPage);

            //if already got 3 valid sellers, don't need to continue
            if (sellers.size() >= 3) {
                break;
            }

            //if last seller is too expensive, don't need to continue
            if (!sellerFilter.profitQualified(lastSeller, order)) {
                break;
            }

            url = nextPage(document, country);
            if (url == null) {
                break;
            }
            WaitTime.Shortest.execute();
        }

        return sellers;
    }

    public List<Seller> getAllSellers(Country country, String asin, Condition condition) {
        List<Seller> sellers = new ArrayList<>();

        String url = getOfferListingPageUrl(country, asin, condition);
        for (int i = 0; i < MAX_PAGE; i++) {
            Document document = htmlFetcher.getDocument(url);

            List<Seller> sellersByPage = parseSellers(document, country);

            if (sellersByPage.size() > 0) {
                sellers.addAll(sellersByPage);
            }

            url = nextPage(document, country);
            if (url == null) {
                break;
            }
            WaitTime.Shortest.execute();
        }

        return sellers;
    }

    public List<Seller> parseSellers(Document document, Country country) {
        List<Seller> sellers = new ArrayList<>();
        Elements sellerRows = HtmlParser.selectElementsByCssSelector(document, "div.a-row.a-spacing-mini.olpOffer");

        int index = 0;
        for (Element row : sellerRows) {
            Seller seller = parseSeller(row, country);
            seller.setIndex(index);
            index++;
            // Seller未标价时不做处理
            if (NumberUtils.ZERO.equals(seller.getPrice().getAmount().floatValue())) {
                continue;
            }

            sellers.add(seller);
        }
        return sellers;
    }

    public List<Seller> parseSellers(String html, Country country) {
        Document document = Jsoup.parse(html);
        return parseSellers(document, country);
    }

    public List<Seller> parseSellers(Browser browser, Country country) {
        return parseSellers(browser.getHTML(), country);
    }

    private Seller parseSeller(Element row, Country country) {
        Seller seller = new Seller();
        seller.setOfferListingCountry(country);

        // 是否有Prime标记
        boolean prime = HtmlParser.selectElementsByCssSelectors(row, "span.supersaver > i.a-icon.a-icon-premium",
                "div.olpBadgeContainer > div.olpBadge > span.a-declarative > a.olpFbaPopoverTrigger",
                "span.supersaver > i.a-icon.a-icon-prime").size() > 0;
        seller.setType(prime ? SellerType.Prime : SellerType.Pt);

        String price = HtmlParser.text(row, "span.a-size-large.a-color-price.olpOfferPrice.a-text-bold");
        if (StringUtils.isNotBlank(price)) {
            seller.setPrice(Money.fromText(price, country));
        } else {
            seller.setPrice(new Money(NumberUtils.ZERO, country));
        }

        // 运费
        String shippingFee = HtmlParser.text(row, "span.olpShippingPrice");
        if (StringUtils.isNotBlank(shippingFee)) {
            seller.setShippingFee(Money.fromText(shippingFee, country));
        } else {
            seller.setShippingFee(new Money(NumberUtils.ZERO, country));
        }

        // Condition和详情
        seller.setCondition(Condition.parseFromText(HtmlParser.text(row, ".olpCondition")));
        seller.setConditionDetail(HtmlParser.text(row, "div.a-column.a-span3 > div.comments"));


        // Seller名称和UUID
        seller.setName(HtmlParser.text(row, ".olpSellerName"));
        Element sellerLink = HtmlParser.selectElementByCssSelector(row, ".olpSellerName a");
        if (sellerLink != null) {
            String sellerProfileUrl = sellerLink.attr(PageUtils.HREF);
            seller.setRatingUrl(sellerProfileUrl);
            String sellerId = PageUtils.getSellerUUID(sellerProfileUrl);
            seller.setUuid(sellerId);

            if (StringUtils.isBlank(seller.getName()) && StringUtils.isNotBlank(sellerId) &&
                    Boolean.TRUE.equals(wareHouseIdCache.get(sellerId))) {
                seller.setType(SellerType.APWareHouse);
            }
        } else if (StringUtils.isBlank(seller.getName())) {
            seller.setType(SellerType.AP);
            seller.setName(SellerType.AP.name());
            seller.setUuid(StringUtils.EMPTY);
        }

        // 库存信息
        String stockText = HtmlParser.text(row, "div.a-column.a-span3.olpDeliveryColumn > ul.a-vertical  li  span.a-list-item");
        seller.setStockStatusFromText(stockText);

        //edd
        I18N_AMAZON.setLocale(country.locale());
        final String arrivesText = I18N_AMAZON.getText("shipping.arrives", country);
        if (StringUtils.startsWithIgnoreCase(stockText, arrivesText)) {
            seller.setLatestDeliveryDate(parseEdd(stockText, country));
        }

        // 是否为AddOn
        if (HtmlParser.select(row, "i.a-icon.a-icon-addon") != null) {
            seller.setAddOn(true);
        }

        // 快递是否可用、运输详情信息(目前暂时不对日本进行校验)
        String exEnabled = "shipping.ex.enabled";
        if (country != Country.JP) {
            exEnabled = I18N_AMAZON.getText("shipping.ex.enabled", country);
        }

        // two day shipping 也算是快递
        String twodayshippingEnabledText = I18N_AMAZON.getText("shipping.twoday.enabled", country);
        String intlEnabledText = "shipping.intl.enabled";
        String shipsFromText = "shipping.from";

        if (country != Country.JP) {
            intlEnabledText = I18N_AMAZON.getText("shipping.intl.enabled", country);
            shipsFromText = I18N_AMAZON.getText("shipping.from", country);
        }

        Elements deliveryInfos = HtmlParser.selectElementsByCssSelector(row,
                "div.a-column.a-span3.olpDeliveryColumn > ul.a-vertical  li  span.a-list-item");
        for (Element line : deliveryInfos) {
            String txt = HtmlParser.text(line);

            if (txt.startsWith(shipsFromText)) {
                // 形如: Ships from CO, United States. Ships from United Kingdom.
                String from = txt;
                if (txt.indexOf('.') != -1) {
                    from = txt.substring(0, txt.indexOf('.'));
                }
                from = from.replace(shipsFromText, StringUtils.EMPTY);
                String[] arr = StringUtils.split(from, ',');
                if (arr.length >= 2) {
                    seller.setShippingFromState(arr[0].trim());
                    seller.setShipFromCountry(getShipFromCountry(arr[1].trim(), country));
                } else {
                    seller.setShipFromCountry(getShipFromCountry(arr[0].trim(), country));
                }
            } else if (StringUtils.contains(txt, exEnabled) || StringUtils.contains(txt, twodayshippingEnabledText)) {
                seller.setExpeditedAvailable(true);
            } else if (StringUtils.contains(txt, intlEnabledText)) {
                seller.setIntlShippingAvailable(true);
            }
        }


        if (seller.getShippingFromCountry() == null && StringUtils.isBlank(seller.getShippingFromState())) {
            Country shipFromCountry = getShipFromCountry(HtmlParser.text(row, "div.a-column.a-span3.olpDeliveryColumn"), country);
            seller.setShipFromCountry(shipFromCountry);
        }

        // 综合Ratings及Ratings总数，AP一般没有这项属性
        if (seller.getType() == SellerType.AP) {
            seller.setRating(Rating.AP_POSITIVE);
            seller.setRatingCount(Rating.AP_COUNT);
        } else {
            try {
                Integer rating = Integer.parseInt(
                        HtmlParser.text(row, "div.a-column.a-span2.olpSellerColumn > p:nth-child(2) > a")
                                .replaceAll(RegexUtils.Regex.NON_DIGITS.val(), StringUtils.EMPTY).trim());
                seller.setRating(rating);
            } catch (Exception e) {
                LOGGER.error("Cant get rating number for seller {}", seller.getName());
            }

            String ratingText = HtmlParser.text(row, "div.a-column.a-span2.olpSellerColumn > p:nth-child(2)");

            if (ratingText.indexOf('(') != -1 && ratingText.indexOf(')') != -1) {
                try {
                    Integer ratingCount = Integer.parseInt(ratingText.substring(ratingText.indexOf('('))
                            .replaceAll(RegexUtils.Regex.NON_DIGITS.val(), StringUtils.EMPTY));
                    seller.setRatingCount(ratingCount);
                } catch (Exception e) {
                    //e.printStackTrace();
                }

            }
        }

        seller.autoCorrect();
        return seller;
    }


    public Date parseEdd(String text, Country country) {
        //Arrives between Jan. 26 - Feb. 12. Read more
        try {
            String[] parts = text.split("-");
            String[] monthDay = parts[1].split("\\.");
            String dateString = monthDay[0].trim() + " " + monthDay[1].trim();

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", country.locale());

            Date date = dateFormat.parse(dateString);
            int years = Dates.getYear(now.get()) - 1970;
            if (Dates.getField(date, Calendar.MONTH) < Dates.getField(now.get(), Calendar.MONTH)) {
                years += 1;
            }

            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.YEAR, years);
            date = c.getTime();
            return date;
        } catch (ParseException e) {
            //LOGGER.error("", e);
            //ignore
            //throw new BusinessException(e);
        }

        return null;
    }

    public void getSellerRatings(Seller seller, Order order) {
        if (seller.isAP()) {
            seller.setRatings(Rating.apRatings());
            return;
        }

        if (StringUtils.isBlank(seller.getRatingUrl())) {
            return;
        }
        try {
            Document document = htmlFetcher.getDocument(seller.getRatingUrl());
            LOGGER.saveHtml(order, seller.getName() + " " + seller.getUuid(), document.outerHtml());
            Map<RatingType, Rating> ratings = getSellerRatings(document);
            seller.setRatings(ratings);
        } catch (Exception e) {
            //
        }


    }


    public Map<RatingType, Rating> getSellerRatings(Document document) {
        Map<RatingType, Rating> ratings = new HashMap<>();

        Element ratingTableElement = HtmlParser.select(document, "#feedback-summary-table");
        if (ratingTableElement == null) {
            LOGGER.error("no feedback table found");
            return ratings;
        }

        Elements trs = HtmlParser.elements(ratingTableElement, "tr");
        //positive - 2nd row, total - 5th row
        Elements positiveTds = HtmlParser.elements(trs.get(1), "td");
        Elements totalTds = HtmlParser.elements(trs.get(4), "td");

        for (RatingType type : RatingType.values()) {
            Rating rating = new Rating(type);
            String positiveText = positiveTds.get(type.getIndex()).text().replaceAll(Regex.NON_DIGITS.val(), "");
            try {
                rating.setPositive(Integer.parseInt(positiveText));
            } catch (Exception e) {
                rating.setPositive(0);
            }

            String totalText = totalTds.get(type.getIndex()).text().replaceAll(Regex.NON_DIGITS.val(), "");
            try {
                rating.setCount(Integer.parseInt(totalText));
            } catch (Exception e) {
                rating.setCount(0);
            }

            ratings.put(type, rating);
        }


        return ratings;
    }

    public Country getShipFromCountry(String shipFromCountryString, Country offerListingCountry) {

        if (StringUtils.isBlank(shipFromCountryString)) {
            return offerListingCountry;
        }

        for (Country country : Country.values()) {
            String countryName = I18N_AMAZON.getText(CountryStateUtils.getInstance().getCountryName(country.code()), offerListingCountry);
            if (StringUtils.containsIgnoreCase(shipFromCountryString, countryName)) {
                return country;
            }
        }

        return offerListingCountry;
    }

    private String getOfferListingPageUrl(Country country, String asin, Condition condition) {
        String urlFormat = "%s/gp/offer-listing/%s/ref=olp_f_used?ie=UTF8&f_all=true&%s";
        return String.format(urlFormat, country.baseUrl(), asin, condition.used() ? "f_new=true&f_used=true" : "f_new=true");
    }

    private String nextPage(Document document, Country country) {

        Element nextPageLink = HtmlParser.selectElementByCssSelector(document, "#olpOfferListColumn .a-pagination li.a-last a");

        if (nextPageLink == null) {
            return null;
        }

        String url = nextPageLink.attr(PageUtils.HREF);
        if (!StringUtils.containsIgnoreCase(url, "http://") && !StringUtils.containsIgnoreCase(url, "https://")) {
            url = country.baseUrl() + "/" + url;
        }

        return url;
    }

}
