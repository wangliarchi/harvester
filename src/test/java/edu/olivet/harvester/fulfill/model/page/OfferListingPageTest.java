package edu.olivet.harvester.fulfill.model.page;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.Seller;
import edu.olivet.harvester.fulfill.service.SellerService;
import edu.olivet.harvester.ui.BuyerPanel;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;

import static org.testng.Assert.*;

public class OfferListingPageTest extends BaseTest {

    @Inject SellerService sellerService;
    @Test
    public void findSeller() {
        order = prepareOrder();
        order.seller = "";
        order.seller_id = "";
        order.character = "AP";
        order.isbn = "1596981091";
        order.sheetName = "11/28";
        Account buyer = new Account("jxiang@olivetuniversity.edu/q1w2e3AA", Account.AccountType.Buyer);
        BuyerPanel buyerPanel = new BuyerPanel(0, Country.US, buyer, 1);


        JFrame frame = new JFrame("Order Submission Demo");
        frame.getContentPane().add(buyerPanel);
        frame.setVisible(true);
        frame.setSize(new Dimension(1260, 736));

        OfferListingPage offerListingPage = new OfferListingPage(buyerPanel, sellerService);
        offerListingPage.enter(order);

        Seller seller = offerListingPage.findSeller(order);
        assertEquals(seller.getName(),"AP");

    }

}