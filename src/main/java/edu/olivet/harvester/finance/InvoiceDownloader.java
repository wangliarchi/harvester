package edu.olivet.harvester.finance;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.finance.model.DownloadParams;
import edu.olivet.harvester.hunt.utils.SellerHuntUtils;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/1/2018 1:37 PM
 */
public class InvoiceDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(InvoiceDownloader.class);

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();

    public void download(DownloadParams downloadParams) {

        for (Account account : downloadParams.getBuyerAccounts()) {
            long start = System.currentTimeMillis();
            messagePanel.wrapLineMsg(String.format("Starting download invoice  from %s at %s.",
                    account, Dates.toDateTime(start)), LOGGER);

            for (Country country : SellerHuntUtils.countriesToHunt()) {

            }


        }
    }
}
