package edu.olivet.harvester.spreadsheet;


import com.alibaba.fastjson.annotation.JSONField;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.spreadsheet.exceptions.NoOrdersFoundInWorksheetException;
import edu.olivet.harvester.spreadsheet.exceptions.NoWorksheetFoundException;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * reprensent a google spreadsheet
 */

@Data
public class Spreadsheet {

    private static final Logger LOGGER = LoggerFactory.getLogger(Spreadsheet.class);

    /**
     *google spreadsheet id
     */
    @JSONField(name = "spreadId")
    @Getter
    private String spreadsheetId;

    /*
    spreadsheet name
     */
    @JSONField(name = "spreadName")
    @Getter
    private String title;

    @Getter
    private List<String> sheetNames;



    public List<String> getOrderSheetNames(){
        List<String> filteredSheetNames = sheetNames;

        String[] notValidOrderSheets = {"daily cost","confirm","template","cs","memo"};
        filteredSheetNames.removeIf(p -> Arrays.asList(notValidOrderSheets).contains(p.toLowerCase()));

        return filteredSheetNames;

    }




    /**
     * each spreadsheet holds certain type of order items: BOOK, PRODUCT or both
     * @return
     */
    public OrderEnums.OrderItemType getSpreadsheetType() {
        List<Settings.Configuration> configs = Settings.load().getConfigs();

        for (Settings.Configuration config : configs) {
            //bookDataSourceUrl
            //productDataSourceUrl

            if (config.getSpreadId(OrderEnums.OrderItemType.BOOK) .equals( this.spreadsheetId) ) {
                return OrderEnums.OrderItemType.BOOK;
            }

            if (config.getSpreadId(OrderEnums.OrderItemType.PRODUCT).equals(spreadsheetId)) {
                return OrderEnums.OrderItemType.PRODUCT;
            }
        }

        throw new BusinessException("Spreadsheet id "+spreadsheetId+" is not in configuration file.");

    }


    public Country getSpreadsheetCountry() {
        List<Settings.Configuration> configs = Settings.load().getConfigs();

        for (Settings.Configuration config : configs) {
            //bookDataSourceUrl
            //productDataSourceUrl
            if (config.getSpreadId(OrderEnums.OrderItemType.BOOK).equals(this.spreadsheetId) ) {
                return config.getCountry();
            }

            if (config.getSpreadId(OrderEnums.OrderItemType.PRODUCT).equals(spreadsheetId)) {
                return config.getCountry();
            }
        }

        throw new BusinessException("Spreadsheet id "+spreadsheetId+" is not in configuration file. No country info found.");

    }







}
