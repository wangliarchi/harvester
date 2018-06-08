package edu.olivet.harvester.feeds.service;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.FeedUploader;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.feeds.helper.FeedGenerator.BatchFileType;
import edu.olivet.harvester.utils.Settings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 3:45 PM
 */

public class FeedUploadService extends FeedUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedUploadService.class);

    public String submitFeedAllZoneCountries(File feedFile, BatchFileType fileType, Country country) {
        String result = "";

        if (country.europe()) {
            for (Country c : Country.EURO) {
                try {
                    result += _submitFeed(feedFile, fileType, c) + "\n";
                    LOGGER.debug("feed result {}", result);
                } catch (Exception e) {
                    result += "Feed submission error " + Strings.getExceptionMsg(e) + "\n";
                    LOGGER.error("Feed submission error {}", e);
                }
            }

            return result;
        }

        return submitFeed(feedFile, fileType, country);
    }

    public String submitFeed(File feedFile, BatchFileType fileType, Country country) {
        messagePanel.displayMsg("Feed submitted to Amazon... It may take few minutes for Amazon to process.");
        String result = "";
        String error = "";
        if (country.europe()) {
            for (Country c : Country.EURO) {
                try {
                    result = _submitFeed(feedFile, fileType, c);
                    LOGGER.debug("feed result {}", result);
                    if (!Strings.containsAnyIgnoreCase(result != null ? result.toLowerCase() : null, "rejected", "denied")) {
                        break;
                    }
                } catch (Exception e) {
                    error = "Feed submission error " + Strings.getExceptionMsg(e);
                    LOGGER.error("Feed submission error {}", e);
                    if (!Strings.containsAnyIgnoreCase(e.getMessage().toLowerCase(), "rejected", "denied")) {
                        break;
                    }
                }
            }
        } else {
            result = _submitFeed(feedFile, fileType, country);
        }

        if (StringUtils.isNotBlank(error)) {
            messagePanel.wrapLineMsg(error, LOGGER, InformationLevel.Negative);
        } else {
            messagePanel.wrapLineMsg("Feed has been submitted successfully. " + result, LOGGER, InformationLevel.Important);
        }

        return result;
    }


    private String _submitFeed(File feedFile, BatchFileType fileType, Country country) {

        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            MarketWebServiceIdentity credential;
            if (country.europe()) {
                credential = Settings.load().getConfigByCountry(Country.UK).getMwsCredential();
                credential.setMarketPlaceId(country.marketPlaceId());
            } else {
                credential = Settings.load().getConfigByCountry(country).getMwsCredential();
            }

            LOGGER.info("Submitting feed to amazon {}, using credential {}", country.name(), credential.toString());
            try {
                return execute(feedFile, fileType.feedType(), credential, 1);
            } catch (Exception e) {
                LOGGER.error("", e);
                if (!isRepeatable(e) || i == Constants.MAX_REPEAT_TIMES - 1) {
                    throw e;
                }
            }
        }
        return null;
    }

    private static final String[] RECOVERABLE_ERROR_MESSAGES = {"Request is throttled",
            "You exceeded your quota",
            "Internal Error",
            "Failed to retrieve batch id"};

    private boolean isRepeatable(Exception e) {
        return Strings.containsAnyIgnoreCase(e.getMessage(), RECOVERABLE_ERROR_MESSAGES);
    }
}
