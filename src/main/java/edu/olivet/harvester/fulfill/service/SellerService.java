package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.I18N;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.fulfill.model.Rating;
import edu.olivet.harvester.fulfill.model.Seller;
import edu.olivet.harvester.fulfill.model.SellerEnums;
import edu.olivet.harvester.fulfill.utils.ConditionUtils;
import edu.olivet.harvester.model.Money;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.order.PageUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/31/17 11:38 AM
 */
public class SellerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerService.class);

    private static final I18N I18N_AMAZON = new I18N("i18n/Amazon");
    private Map<String, Boolean> wareHouseIdCache = new HashMap<>();

    @Inject
    public void init() throws IOException {
        Map<String, String> wareHouseIds = Configs.load("wrsellerids.properties");
        for (Map.Entry<String, String> entry : wareHouseIds.entrySet()) {
            wareHouseIdCache.put(entry.getValue(), true);
        }
    }

    public List<Seller> parseSellers(Browser browser, Country country) {
        List<Seller> sellers = new ArrayList<>();
        List<DOMElement> sellerRows = JXBrowserHelper.selectElementsByCssSelector(browser, "div.a-row.a-spacing-mini.olpOffer");

        int index = 0;
        for (DOMElement row : sellerRows) {
            Seller seller = parseSeller(row, country);
            seller.setIndex(index);
            index++;
            // Seller未标价时不做处理
            if (ZERO.equals(seller.getPrice().getAmount().floatValue())) {
                continue;
            }

            sellers.add(seller);
        }


        return sellers;

    }


    private static final Float ZERO = 0.0f;

    private Seller parseSeller(DOMElement row, Country country) {
        Seller seller = new Seller();
        seller.setOfferListingCountry(country);

        // 是否有Prime标记
        boolean prime =
                JXBrowserHelper.selectElementsByCssSelectors(row, "span.supersaver > i.a-icon.a-icon-premium",
                        "div.olpBadgeContainer > div.olpBadge > span.a-declarative > a.olpFbaPopoverTrigger",
                        "span.supersaver > i.a-icon.a-icon-prime").size() > 0;
        seller.setType(prime ? SellerEnums.SellerType.Prime : SellerEnums.SellerType.Pt);

        String price = JXBrowserHelper.text(row, "span.a-size-large.a-color-price.olpOfferPrice.a-text-bold");

        if (StringUtils.isNotBlank(price)) {
            seller.setPrice(Money.fromText(price, country));
        } else {
            seller.setPrice(new Money(ZERO, country));
        }

        // 运费
        String shippingFee = JXBrowserHelper.text(row, "span.olpShippingPrice");
        if (StringUtils.isNotBlank(shippingFee)) {
            seller.setShippingFee(Money.fromText(shippingFee, country));
        } else {
            seller.setShippingFee(new Money(ZERO, country));
        }

        // Condition和详情
        seller.setCondition(ConditionUtils.translateCondition(JXBrowserHelper.text(row, ".olpCondition")));
        seller.setConditionDetail(JXBrowserHelper.text(row, "div.a-column.a-span3 > div.comments"));


        // Seller名称和UUID
        seller.setName(JXBrowserHelper.text(row, ".olpSellerName"));
        DOMElement sellerLink = JXBrowserHelper.selectElementByCssSelector(row, ".olpSellerName a");
        if (sellerLink != null) {
            String sellerId = PageUtils.getSellerUUID(sellerLink.getAttribute(PageUtils.HREF));
            seller.setUuid(sellerId);
            if (StringUtils.isBlank(seller.getName()) && StringUtils.isNotBlank(sellerId) &&
                    Boolean.TRUE.equals(wareHouseIdCache.get(sellerId))) {
                seller.setType(SellerEnums.SellerType.APWareHouse);
            }
        } else if (StringUtils.isBlank(seller.getName())) {
            seller.setType(SellerEnums.SellerType.AP);
            seller.setName(SellerEnums.SellerType.AP.name());
            seller.setUuid(StringUtils.EMPTY);
        }

        // 库存信息
        String stockText = JXBrowserHelper.text(row, "div.a-column.a-span3.olpDeliveryColumn > ul.a-vertical > li > span.a-list-item");
        seller.setStockText(stockText);

        // 是否为AddOn
        if (JXBrowserHelper.selectElementsByCssSelector(row, "i.a-icon.a-icon-addon") != null) {
            seller.setAddOn(true);
        }

        // 快递是否可用、运输详情信息(目前暂时不对日本进行校验)
        String exEnabled = "shipping.ex.enabled";
        if (!country.equals(Country.JP)) {
            exEnabled = I18N_AMAZON.getText("shipping.ex.enabled", country);
        }

        // two day shipping 也算是快递
        String twodayshippingEnabled = I18N_AMAZON.getText("shipping.twoday.enabled", country);

        String intlEnabled = "shipping.intl.enabled";
        String shipsFrom = "shipping.from";

        if (country != Country.JP) {
            intlEnabled = I18N_AMAZON.getText("shipping.intl.enabled", country);
            shipsFrom = I18N_AMAZON.getText("shipping.from", country);
        }

        List<DOMElement> deliveryInfos = JXBrowserHelper.selectElementsByCssSelector(row,
                "div.a-column.a-span3.olpDeliveryColumn > ul.a-vertical > li > span.a-list-item");
        for (DOMElement line : deliveryInfos) {
            String txt = line.getInnerText().trim();

            // 查找有没有not on stock等字样
            seller.setInstock(txt);

            if (txt.startsWith(shipsFrom)) {
                // 形如: Ships from CO, United States. Ships from United Kingdom.
                String from = txt;
                if (txt.indexOf('.') != -1) {
                    from = txt.substring(0, txt.indexOf('.'));
                }
                from = from.replace(shipsFrom, StringUtils.EMPTY);

                String[] arr = StringUtils.split(from, ',');
                if (arr.length >= 2) {
                    seller.setShippingFromState(arr[0].trim());
                    seller.setShipFromCountry(arr[1].trim());
                } else {
                    seller.setShipFromCountry(arr[0].trim());
                }


            } else if (StringUtils.contains(txt, exEnabled) || StringUtils.contains(txt, twodayshippingEnabled)) {
                seller.setExpeditedAvailable(true);
            } else if (StringUtils.contains(txt, intlEnabled)) {
                seller.setIntlShippingAvailable(true);
            }
        }


        if (seller.getShippingFromCountry() == null && StringUtils.isBlank(seller.getShippingFromState())) {
            seller.setShipFromCountry(JXBrowserHelper.text(row, "div.a-column.a-span3.olpDeliveryColumn"));
        }

        if (SellerEnums.SellerType.isPrime(seller.getType())) {
            seller.setExpeditedAvailable(true);
        }
        if (seller.getType() == SellerEnums.SellerType.AP) {
            seller.setIntlShippingAvailable(true);
        }


        // 综合Ratings及Ratings总数，AP一般没有这项属性
        if (seller.getType() == SellerEnums.SellerType.AP) {
            seller.setRating(Rating.AP_POSITIVE);
            seller.setRatingCount(Rating.AP_COUNT);
        } else {
            try {
                Integer rating = Integer.parseInt(
                        JXBrowserHelper.text(row, "div.a-column.a-span2.olpSellerColumn > p:nth-child(2) > a")
                                .replaceAll(RegexUtils.Regex.NON_DIGITS.val(), StringUtils.EMPTY).trim());
                seller.setRating(rating);
            } catch (Exception e) {
                LOGGER.error("Cant get rating number for seller {}", seller.getName());
            }

            String ratingText = JXBrowserHelper.text(row, "div.a-column.a-span2.olpSellerColumn > p:nth-child(2)");

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


}
