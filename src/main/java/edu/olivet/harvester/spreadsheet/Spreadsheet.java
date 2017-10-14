package edu.olivet.harvester.spreadsheet;


import com.alibaba.fastjson.annotation.JSONField;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.model.OrderEnums;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

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

    @Setter
    private OrderEnums.OrderItemType spreadsheetType;


    static final String[] INVALID_ORDER_SHEETS = {"daily cost", "confirm", "template", "cs", "memo"};

    public List<String> getOrderSheetNames() {
      return sheetNames.stream().filter(p -> ArrayUtils.contains(INVALID_ORDER_SHEETS,p.toLowerCase()) == false).collect(Collectors.toList());
    }

    public OrderEnums.OrderItemType getSpreadsheetType() {
        if (spreadsheetType == null) {
            try {
                spreadsheetType = Settings.load().getSpreadsheetType(spreadsheetId);
            } catch (BusinessException e) {
                throw new BusinessException("Cant load type information for spreadsheet " + spreadsheetId);
            }
        }

        return spreadsheetType;
    }

    public Country getSpreadsheetCountry() {
        if (spreadsheetCountry == null) {
            try {
                spreadsheetCountry = Settings.load().getSpreadsheetCountry(spreadsheetId);
            } catch (BusinessException e) {
                throw new BusinessException("Cant load country information for spreadsheet " + spreadsheetId);
            }
        }

        return spreadsheetCountry;
    }


}
