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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfirmShipmentEmailSender extends EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmShipmentEmailSender.class);


    public void sendErrorFoundEmail(String summary, String errorDescription, Country country, Destination destination) {
        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        String subject = String.format("%s %s Confirm Shipment Report: %s",
                config.getAccountCode(), Dates.today(), summary);


        if (!StringUtils.containsIgnoreCase(destination.toString(), config.getSellerEmail().getEmail())) {
            destination.withCcAddresses(config.getSellerEmail().getEmail());
        }

        try {
            this.sendMessage(subject, errorDescription, destination);
        } catch (BusinessException e) {
            LOGGER.error("Failed to send shipment confirmation success email.{} - {}", subject, e.getMessage());
        }
    }

    public void sendErrorFoundEmail(String summary, String errorDescription, Country country) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        Destination destination = new Destination().withCcAddresses(config.getSellerEmail().getEmail());

        sendErrorFoundEmail(summary, errorDescription, country, destination);
    }

    public void sendErrorFoundEmail(String summary, String errorDescription, Country country, String receptions) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        Destination destination = new Destination().withCcAddresses(config.getSellerEmail().getEmail()).withToAddresses(receptions);

        if (StringUtils.isNotEmpty(receptions)) {
            destination.withToAddresses(receptions);
        }

        sendErrorFoundEmail(summary, errorDescription, country, destination);
    }

    public void sendSuccessEmail(String result, File feedFile, Country country, Destination destination) {
        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        if (!StringUtils.containsIgnoreCase(destination.toString(), config.getSellerEmail().getEmail())) {
            destination.withCcAddresses(config.getSellerEmail().getEmail());
        }


        String title = String.format("%s %s Confirm Shipment Batch File Report: Total %s, Succeed %s, Failed %s",
                config.getAccountCode(), Dates.today(), counts[0], counts[1], counts[2]);

        String content = String.format(
                "Order confirmation feed file %s was uploaded and executed successfully:%n%n%s %n%n Feed file uploaded to Amazon is attached in this email.",
                feedFile.getName(), result);

        try {
            this.sendMessage(destination, title, content, EmailContentType.PlainText, feedFile);
        } catch (BusinessException e) {
            LOGGER.error("Failed to send shipment confirmation success email.{} - {}", title, e.getMessage());
        }

    }

    public void sendSuccessEmail(String result, File feedFile, Country country) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        Destination destination = new Destination().withToAddresses(config.getSellerEmail().getEmail())
                .withCcAddresses(Constants.RND_EMAIL);


        sendSuccessEmail(result, feedFile, country, destination);
    }

    public void sendSuccessEmail(String result, File feedFile, Country country, String receptions) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        Destination destination = new Destination()
                .withCcAddresses(config.getSellerEmail().getEmail())
                .withBccAddresses(Constants.RND_EMAIL);

        if (StringUtils.isNotEmpty(receptions)) {
            destination.withToAddresses(receptions);
        }


        sendSuccessEmail(result, feedFile, country, destination);
    }
}
