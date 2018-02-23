package edu.olivet.harvester.feeds.helper;

import com.amazonaws.services.simpleemail.model.Destination;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.google.EmailContentType;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.feeds.helper.FeedGenerator.BatchFileType;
import edu.olivet.harvester.message.EmailService;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class FeedSubmissionEmailSender extends EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedSubmissionEmailSender.class);


    public String getTypeDesc(BatchFileType fileType) {
        switch (fileType) {
            case ShippingConfirmation:
                return "confirm shipment";
            case ReQuantity:
                return "Update asin qty";
            case ListingDeletion:
                return "ASIN removal";
            default:
                return fileType.name();
        }
    }

    public void sendErrorFoundEmail(BatchFileType fileType, String summary, String errorDescription, Country country, Destination destination) {
        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        String desc = getTypeDesc(fileType);
        String subject = String.format("%s %s %s Report: %s",
                config.getAccountCode(), desc, Dates.today(), summary);


        if (!StringUtils.containsIgnoreCase(destination.toString(), config.getSellerEmail().getEmail())) {
            destination.withCcAddresses(config.getSellerEmail().getEmail());
        }

        try {
            this.sendMessage(subject, errorDescription, destination);
        } catch (BusinessException e) {
            LOGGER.error("Failed to send {} success email.{} - {}", desc, subject, e);
        }
    }

    public void sendErrorFoundEmail(BatchFileType fileType, String summary, String errorDescription, Country country) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        Destination destination = new Destination().withCcAddresses(config.getSellerEmail().getEmail());

        sendErrorFoundEmail(fileType, summary, errorDescription, country, destination);
    }

    public void sendErrorFoundEmail(BatchFileType fileType, String summary, String errorDescription, Country country, String receptions) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);
        Destination destination = new Destination().withCcAddresses(config.getSellerEmail().getEmail()).withToAddresses(receptions);

        if (StringUtils.isNotEmpty(receptions)) {
            destination.withToAddresses(receptions);
        }

        sendErrorFoundEmail(fileType, summary, errorDescription, country, destination);
    }

    public void sendSuccessEmail(BatchFileType fileType, String result, File feedFile, Country country, Destination destination) {
        String desc = getTypeDesc(fileType);
        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        int[] counts = ServiceUtils.parseFeedSubmissionResult(result);

        if (!StringUtils.containsIgnoreCase(destination.toString(), config.getSellerEmail().getEmail())) {
            destination.withCcAddresses(config.getSellerEmail().getEmail());
        }

        String title = String.format("%s %s %s Batch File Report: Total %s, Succeed %s, Failed %s",
                config.getAccountCode(), Dates.today(), StringUtils.capitalize(desc), counts[0], counts[1], counts[2]);

        String content = String.format(
                "%s feed file %s was uploaded and executed successfully:%n%n%s %n%n Feed file uploaded to Amazon is attached in this email.",
                StringUtils.capitalize(desc), feedFile.getName(), result);

        try {
            this.sendMessage(destination, title, content, EmailContentType.PlainText, feedFile);
        } catch (BusinessException e) {
            LOGGER.error("Failed to send %s success email.{} - {}", desc.toLowerCase(), title, e);
        }

    }

    public void sendSuccessEmail(BatchFileType fileType, String result, File feedFile, Country country) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        Destination destination = new Destination().withToAddresses(config.getSellerEmail().getEmail())
                .withCcAddresses(Constants.RND_EMAIL);


        sendSuccessEmail(fileType, result, feedFile, country, destination);
    }

    public void sendSuccessEmail(BatchFileType fileType, String result, File feedFile, Country country, String receptions) {

        Settings.Configuration config = Settings.load().getConfigByCountry(country);

        Destination destination = new Destination()
                .withCcAddresses(config.getSellerEmail().getEmail())
                .withBccAddresses(Constants.RND_EMAIL);

        if (StringUtils.isNotEmpty(receptions)) {
            destination.withToAddresses(receptions);
        }


        sendSuccessEmail(fileType, result, feedFile, country, destination);
    }
}
