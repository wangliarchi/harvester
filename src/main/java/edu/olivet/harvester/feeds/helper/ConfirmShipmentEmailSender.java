package edu.olivet.harvester.feeds.helper;

import com.amazonaws.services.simpleemail.model.Destination;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.google.EmailContentType;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.message.EmailService;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfirmShipmentEmailSender extends EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmShipmentEmailSender.class);

    public void sendErrorFoundEmail(String summary, String errorDescription, Country country) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        String subject = String.format("%s %s Confirm Shipment Report: %s",
                config.getAccountCode(), Dates.today(), summary);

        try {
            this.sendMessage(subject, errorDescription, new Destination().withToAddresses(config.getSellerEmail().getEmail()));
        } catch (BusinessException e) {
            LOGGER.error("Failed to send shipment confirmation success email.{} - {}", subject, e.getMessage());
        }

    }


    public void sendSuccessEmail(String result, File feedFile, Country country) {

        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        String title = String.format("%s %s Confirm Shipment Batch File Report: Total %s, Succeed %s, Failed %s",
                config.getAccountCode(), Dates.today(), counts[0], counts[1], counts[2]);

        String content = String.format(
            "Order confirmation feed file %s was uploaded and executed successfully:%n%n%s %n%n Feed file uploaded to Amazon is attached in this email.",
                feedFile.getName(), result);

        Destination destination = new Destination().withToAddresses(config.getSellerEmail().getEmail())
                .withCcAddresses(Constants.RND_EMAIL);

        try {
            this.sendMessage(destination, title, content, EmailContentType.PlainText, feedFile);
        } catch (BusinessException e) {
            LOGGER.error("Failed to send shipment confirmation success email.{} - {}", title, e.getMessage());
        }
    }
}
