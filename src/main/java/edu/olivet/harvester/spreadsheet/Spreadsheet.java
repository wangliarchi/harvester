package edu.olivet.harvester.spreadsheet;


import com.alibaba.fastjson.annotation.JSONField;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * reprensent a google spreadsheet
 */

@Data
public class Spreadsheet {

    private static final Logger LOGGER = LoggerFactory.getLogger(Spreadsheet.class);

    /**
     * google spreadsheet id
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

    @Setter
    private Country spreadsheetCountry;

    @Getter
    @Setter
    private OrderEnums.OrderItemType spreadsheetType;


    public List<String> getOrderSheetNames() {
        List<String> filteredSheetNames = sheetNames;

        String[] notValidOrderSheets = {"daily cost", "confirm", "template", "cs", "memo"};
        filteredSheetNames.removeIf(p -> Arrays.asList(notValidOrderSheets).contains(p.toLowerCase()));

        return filteredSheetNames;
    }

    public Country getSpreadsheetCountry() {
        if (spreadsheetCountry == null) {
            try {
                setSpreadsheetCountry(Settings.load().getSpreadsheetCountry(spreadsheetId));
            } catch (BusinessException e) {
                throw new BusinessException("Cant load country information for spreadsheet " + spreadsheetId);
            }
        }

        return spreadsheetCountry;
    }


}
