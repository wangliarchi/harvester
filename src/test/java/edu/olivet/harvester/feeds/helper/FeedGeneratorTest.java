package edu.olivet.harvester.feeds.helper;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.model.OrderEnums;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

@Guice
public class FeedGeneratorTest {

    @Inject private FeedGenerator feedGenerator;

    @Test
    public void testGenerateConfirmShipmentFeedFromRows() throws Exception {
        List<String[]> orders = new ArrayList<>();
        orders.add(new String[]{"112-4293495-9496221","USPS","","2017-09-22"});
        orders.add(new String[]{"701-5618634-6130633","Other","DHL eCommerce","2017-09-22"});
        orders.add(new String[]{"113-0763482-1323467","USPS","","2017-09-22"});

        File feedFile = feedGenerator.generateConfirmShipmentFeedFromRows(orders, Country.US, OrderEnums.OrderItemType.BOOK);



        //assertTrue(feedFile.getAbsolutePath().contains("app-data/feeds/US_BOOK_ShippingConfirmation"));

        assertEquals(Tools.readFileToString(feedFile),"order-id\tcarrier-code\tcarrier-name\tship-date\n" +
                "112-4293495-9496221\tUSPS\t\t2017-09-22\n" +
                "701-5618634-6130633\tOther\tDHL eCommerce\t2017-09-22\n" +
                "113-0763482-1323467\tUSPS\t\t2017-09-22\n");
    }

}