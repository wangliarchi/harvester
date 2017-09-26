package edu.olivet.harvester.feeds.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.utils.Csv;
import org.nutz.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Singleton
public class FeedGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedGenerator.class);
    @Inject
    private Csv csvHelper;

    public File generateConfirmShipmentFeedFromRows(List<String[]> orders, Country country, OrderEnums.OrderItemType type)  {

        //clear existed data
        csvHelper.clear();


        File file = this.initReportFile("confirm_shipment",country,type);

        String[] header = {"order-id", "carrier-code", "ship-date"};


        csvHelper.addRowFromArray(header,"\t");


        for(String[] row : orders) {
            csvHelper.addRowFromArray(row,"\t");
        }

        return csvHelper.saveToFile(file);
    }


    /**
     * initialize feed file.
     *
     * @param feedType
     * @param country
     * @return
     */
    private File initReportFile(String feedType, Country country, OrderEnums.OrderItemType sheetType) {

        try {
            File file = null;

            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss"); // 设置日期格式

            file = new File(this.getFeedDirectory(), country.name() +"_" + sheetType.name()  + "_" +feedType+ "_" +  df.format(new Date()) + ".txt");

            Tools.createFileIfNotExist(file);

            return file;
        }
        catch (Exception e) {
            LOGGER.error("${message}", e);;;
            throw e;
        }

    }


    private String getFeedDirectory() {
        String feedRootDictoryPath = Directory.APP_DATA + File.separator + "feeds";
        File feedRootFolder = new File(feedRootDictoryPath);

        if (!feedRootFolder.exists()) {
            try{
                feedRootFolder.mkdir();
            }catch (SecurityException e) {
                // -> Ignore
            }
        }


        return feedRootDictoryPath;

    }

}
