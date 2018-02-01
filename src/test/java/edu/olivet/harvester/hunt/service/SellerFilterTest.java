package edu.olivet.harvester.hunt.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import edu.olivet.harvester.hunt.model.Seller;
import org.elasticsearch.common.recycler.Recycler.C;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SellerFilterTest extends BaseTest {

    @Inject SellerFilter sellerFilter;

    @Test
    public void conditionQualified() {
        //New：new；
        //Good：销售价在40美元或40美元以下的选acceptable 以上的condition，
        // 销售价在40美元或40美元以上的不选acceptable，只选good以上的condition；
        //acceptable：acceptable 以上的condition,。
        //针对acc的选择标准：
        // a，1年标注等同正常找单规则；1个月的rating 需要在95%，rating数是10个以上。
        // b，凡是包含water，damage，heavy，loose，ink，stain等字样，都不选；

        order = prepareOrder();
        order.shipping_fee = "3.99";
        Seller seller = new Seller();
        seller.setOfferListingCountry(Country.US);

        //New：new；
        order.original_condition = "New-New";
        seller.setCondition(Condition.New);
        assertTrue(sellerFilter.conditionQualified(seller, order));

        order.original_condition = "Used-Good";
        order.price = "50";
        seller.setCondition(Condition.New);
        assertTrue(sellerFilter.conditionQualified(seller, order));

        seller.setCondition(Condition.UsedGood);
        assertTrue(sellerFilter.conditionQualified(seller, order));

        seller.setCondition(Condition.UsedAcceptable);
        assertFalse(sellerFilter.conditionQualified(seller, order));

        order.price = "30";
        seller.setCondition(Condition.UsedAcceptable);
        seller.setConditionDetail("");
        assertTrue(sellerFilter.conditionQualified(seller, order));

        order.price = "30";
        seller.setCondition(Condition.UsedAcceptable);
        seller.setConditionDetail("Cover differs from one ink shown - Pages are tanned. ");
        assertFalse(sellerFilter.conditionQualified(seller, order));
    }

}