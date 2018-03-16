package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
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
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import edu.olivet.harvester.utils.I18N;
import edu.olivet.harvester.utils.common.DatetimeHelper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:38 AM
 */
public class SellerService {
    private static final SellerHuntingLogger LOGGER = SellerHuntingLogger.getLogger(SellerService.class);
    private static final Logger logger = LoggerFactory.getLogger(SellerService.class);
    private Map<String, Boolean> wareHouseIdCache = new HashMap<>();
    /**
     * 程序找单时默认翻页数上限:{@value}
     */
    public static final int MAX_PAGE = 3;
    private static I18N I18N_AMAZON;

    @Inject Now now;
    @Inject SellerFilter sellerFilter;
    @Inject SellerHuntUtils sellerHuntUtils;

    @Inject
    public void init() throws IOException {
        Map<String, String> wareHouseIds = Configs.load("wrsellerids.properties");
        for (Map.Entry<String, String> entry : wareHouseIds.entrySet()) {
            wareHouseIdCache.put(entry.getValue(), true);
        }
        I18N_AMAZON = new I18N("i18n/Amazon");
        HtmlFetcher.setSilentMode(true);
    }

    public List<Seller> getSellersForOrder(Order order) {
        if (StringUtils.isBlank(order.isbn)) {
            throw new BusinessException("No ISBN found for order yet");
        }

        Map<Country, Set<SellerFullType>> countriesToHunt = sellerHuntUtils.countriesToHunt(order);
        LOGGER.setOrder(order).getLogger().info("Trying to find sellers for {} {} from {} {} - {}",
                order.isbn, order.original_condition,
                countriesToHunt.size(), countriesToHunt.size() == 1 ? "country" : "countries", countriesToHunt);

        final List<Seller> sellers = new ArrayList<>();
        countriesToHunt.forEach((country, types) -> {
            try {
                List<Seller> sellersFromCountry = getSellersByCountry(country, order, types);
                LOGGER.setOrder(order).getLogger().info(String.format("found %d sellers from %s \n", sellersFromCountry.size(), country.name()));
                sellers.addAll(sellersFromCountry);
            } catch (Exception e) {
                LOGGER.setOrder(order).getLogger().info(Strings.getExceptionMsg(e));
            }
        });

        return sellers;
    }


    public List<Seller> getSellersByCountry(Country country, Order order, Set<SellerFullType> allowedTypes) {
        String isbn = Strings.fillMissingZero(order.isbn);
        String url = getOfferListingPageUrl(country, isbn, order.originalCondition());

        final List<Seller> sellers = new ArrayList<>();
        int maxSellerNumber = order.isIntlOrder() ? 5 : 3;

        for (int i = 0; i < MAX_PAGE; i++) {
            Document document;
            try {
                document = HtmlFetcher.getDocument(url);
            } catch (Exception e) {
                continue;
            }

            LOGGER.saveHtml(order, order.isbn + "-offer-listing-page-" + (i + 1), document.outerHtml());

            List<Seller> sellersByPage = parseSellers(document, country);

            LOGGER.setOrder(order).getLogger().info("Found {} sellers on page {} on {}, url {} ", sellersByPage.size(), i + 1, country.baseUrl(), url);
            if (CollectionUtils.isEmpty(sellersByPage)) {
                break;
            }

            Seller lastSeller = sellersByPage.get(sellersByPage.size() - 1);

            //remove unqualified sellers
            sellersByPage.removeIf(seller -> !sellerFilter.isPreliminaryQualified(seller, order) ||
                    !sellerFilter.typeAllowed(seller, order, allowedTypes)
            );

            //get seller ratings on seller profile page
            sellersByPage.forEach(seller -> this.getSellerRatings(seller, order));

            //remove unqualified sellers
            sellersByPage.removeIf(seller -> !sellerFilter.isQualified(seller, order));

            sellers.addAll(sellersByPage);

            //if already got enough valid sellers, don't need to continue
            if (sellers.size() >= maxSellerNumber) {
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
            Document document = HtmlFetcher.getDocument(url);

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
        seller.setLatestDeliveryDate(this.parseEdd(stockText, country));

        // 是否为AddOn
        if (HtmlParser.select(row, "i.a-icon.a-icon-addon") != null) {
            seller.setAddOn(true);
        }

        Elements deliveryInfos = HtmlParser.selectElementsByCssSelector(row,
                "div.a-column.a-span3.olpDeliveryColumn > ul.a-vertical  li  span.a-list-item");
        this.parseDeliveryInfo(country, deliveryInfos, seller);
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
                logger.error("Cant get rating number for seller {}", seller.getName());
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


    public void parseDeliveryInfo(Country country, Elements deliveryInfos, Seller seller) {
        String shipsFromText = I18N_AMAZON.getText("shipping.from", country);
        String twodayshippingEnabledText = I18N_AMAZON.getText("shipping.twoday.enabled", country);
        String intlEnabledText = I18N_AMAZON.getText("shipping.intl.enabled", country);
        String exEnabled = I18N_AMAZON.getText("shipping.ex.enabled", country);

        for (Element line : deliveryInfos) {
            String txt = HtmlParser.text(line);
            if (txt.startsWith(shipsFromText)) {
                // 形如: Ships from CO, United States. Ships from United Kingdom.
                seller.setShipFromCountry(parseShipFromCountry(txt, country));
            } else if (StringUtils.contains(txt, exEnabled) || StringUtils.contains(txt, twodayshippingEnabledText)) {
                seller.setExpeditedAvailable(true);
            } else if (StringUtils.contains(txt, intlEnabledText)) {
                seller.setIntlShippingAvailable(true);
            }
        }


    }

    public Country parseShipFromCountry(String txt, Country country) {
        // 形如: Ships from CO, United States. Ships from United Kingdom.
        String shipsFromText = I18N_AMAZON.getText("shipping.from", country);
        String from = txt;
        if (txt.indexOf('.') != -1) {
            from = txt.substring(0, txt.indexOf('.'));
        }
        from = from.replace(shipsFromText, StringUtils.EMPTY);
        String[] arr = StringUtils.split(from, ',');
        if (arr.length >= 2) {
            return getShipFromCountry(arr[1].trim(), country);
            //seller.setShippingFromState(arr[0].trim());
            //seller.setShipFromCountry(getShipFromCountry(arr[1].trim(), country));
        }
        return getShipFromCountry(arr[0].trim(), country);
    }

    public Date parseEdd(String stockText, Country country) {
        I18N_AMAZON.setLocale(country.locale());
        Date edd = null;
        final String arrivesText = I18N_AMAZON.getText("shipping.arrives", country);
        if (StringUtils.startsWithIgnoreCase(stockText, arrivesText)) {

            String eddText = stockText.substring(0, stockText.lastIndexOf("."));
            try {
                edd = DatetimeHelper.parseEdd(eddText, country, now.get());
            } catch (Exception e) {
                logger.error("{} - {} ", stockText, eddText, e);
            }

        }
        return edd;
        //seller.setLatestDeliveryDate(edd);
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
            Document document = HtmlFetcher.getDocument(seller.getRatingUrl());
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
            logger.error("no feedback table found");
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
