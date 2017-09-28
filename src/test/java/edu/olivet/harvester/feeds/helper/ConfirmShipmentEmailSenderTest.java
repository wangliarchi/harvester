package edu.olivet.harvester.feeds.helper;

import com.amazonaws.services.simpleemail.model.Destination;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;

@Guice
public class ConfirmShipmentEmailSenderTest extends BaseTest {

    private String testEmailAddress = "johnnyxiang2017@gmail.com";

    @Inject
    private ConfirmShipmentEmailSender confirmShipmentEmailSender;

    @BeforeClass
    public  void  init() {
        confirmShipmentEmailSender.setSenderAccount(new Account("johnnyxiang2017@gmail.com/q1w2e3AA",Account.AccountType.Email));
        confirmShipmentEmailSender.setTestMode(true);
        confirmShipmentEmailSender.setTestDestination(new Destination().withToAddresses(testEmailAddress));
    }

    @Test
    public void testSendSuccessEmail() throws Exception {
        String submissionResult = "Feed Processing Summary:\n" +
                "\tNumber of records processed\t\t11\n" +
                "\tNumber of records successful\t\t11";
        File feedFile = new File(TEST_DATA_ROOT+File.separator+"feed-US_BOOK_confirm_shipment_2017-9-28_120813.txt");

        confirmShipmentEmailSender.sendSuccessEmail(submissionResult, feedFile, Country.US);
    }

    @Test
    public void testSendErrorFoundEmail() {
        String subject = "No order found";
        confirmShipmentEmailSender.sendErrorFoundEmail(subject,subject, Country.US);
    }

}