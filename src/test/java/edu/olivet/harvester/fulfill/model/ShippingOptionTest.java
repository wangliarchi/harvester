package edu.olivet.harvester.fulfill.model;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.utils.common.DateFormat;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/21/17 10:21 AM
 */
@Guice(modules = {MockDateModule.class})
public class ShippingOptionTest {
    @Inject
    Now now;

    @Test
    public void testShippingOption() throws Exception {
        now.set(Dates.parseDate("11/18/2017"));


        //
        ShippingOption shippingOption = new ShippingOption("Two-Day Shipping — get it Wednesday, Nov 22", "— get it Wednesday, Nov 22", "", Country.CA, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/22");
        assertTrue(shippingOption.isExpedited());


        shippingOption = new ShippingOption("Standard Delivery : delivered by Dec. 20-22", "delivered by Dec. 20-22", "", Country.UK, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/22");
        assertFalse(shippingOption.isExpedited());

        shippingOption = new ShippingOption("AmazonGlobal Expedited Shipping : delivered by Dec. 12-21", "delivered by Dec. 12-21", "", Country.UK, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/21");
        assertTrue(shippingOption.isExpedited());

        shippingOption = new ShippingOption("AmazonGlobal Priority : delivered by Monday, Dec. 11", "delivered by Monday, Dec. 11", "", Country.UK, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/11");
        assertTrue(shippingOption.isExpedited());

        //get it Tomorrow, Dec 2, by 9pm
        shippingOption = new ShippingOption("One-Day Delivery — get it Tomorrow, Nov 19, by 9pm", "— get it Tomorrow, Nov 19, by 9pm", "", Country.CA, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/19");
        assertTrue(shippingOption.isExpedited());


        shippingOption = new ShippingOption("Two-Day Shipping --get it Nov 22 - 23", "--get it Wednesday, Nov  22 - 23", "", Country.CA, now.get());
        assertEquals(shippingOption.getPrice(), null);
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/23");
        assertTrue(shippingOption.isExpedited());

        shippingOption = new ShippingOption("Friday, Nov. 24 - Tuesday, Nov. 28", "$2.99 - Expedited Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$2.99");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/28");
        assertTrue(shippingOption.isExpedited());


        shippingOption = new ShippingOption("Monday, Nov. 27 - Friday, Dec. 1", "FREE Standard Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$0.00");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/01");
        assertFalse(shippingOption.isExpedited());

        shippingOption = new ShippingOption("Sunday, Nov. 19", "$5.99 - One-Day Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$5.99");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/19");
        assertTrue(shippingOption.isExpedited());

        shippingOption = new ShippingOption("Monday, Nov. 20", "FREE Two-Day Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$0.00");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/20");
        assertTrue(shippingOption.isExpedited());

        shippingOption = new ShippingOption("Wednesday, Nov. 29", "FREE No-Rush Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$0.00");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "11/29");
        assertFalse(shippingOption.isExpedited());


        shippingOption = new ShippingOption("averages 9-12 business days", "$14.99 - Standard Intl Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$14.99");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/05");
        assertFalse(shippingOption.isExpedited());


        shippingOption = new ShippingOption("averages 9-12 business days", "$14.99 - Standard Intl Shipping", Country.US, now.get());
        assertEquals(shippingOption.getPrice().toString(), "$14.99");
        assertEquals(Dates.format(shippingOption.getLatestDeliveryDate(), DateFormat.FULL_MONTH_DAY.pattern()), "12/05");
        assertFalse(shippingOption.isExpedited());


    }


}