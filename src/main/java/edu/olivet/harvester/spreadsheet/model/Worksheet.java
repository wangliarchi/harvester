package edu.olivet.harvester.spreadsheet.model;

import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.utils.ServiceUtils;
import lombok.Data;
import lombok.Getter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Data
public class Worksheet {

    @Getter
    private String sheetName;

    @Getter
    private Spreadsheet spreadsheet;

    public Worksheet(Spreadsheet spreadsheet, String sheetName) {
        this.spreadsheet = spreadsheet;
        this.sheetName = sheetName;
    }

    public String getOrderConfirmationDate() {
        //current worksheet date
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date now = new Date();
        ZoneId zoneId = ServiceUtils.getTimeZone(spreadsheet.getSpreadsheetCountry()).toZoneId();
        LocalDate localDate = now.toInstant().atZone(zoneId).toLocalDate();

        Date sheetDate;
        LocalDate sheetLocalDate;
        try {
            sheetDate = df.parse(localDate.getYear() + "/" + this.getSheetName() + " 07:00:00");
            sheetLocalDate = sheetDate.toInstant().atZone(zoneId).toLocalDate();
        } catch (ParseException e) {
            throw new BusinessException("Sheet with name " + this.getSheetName() + " is not valid");
        }


        DateTimeFormatter feedDf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return feedDf.format(sheetLocalDate);

    }

    public String toString() {
        return this.spreadsheet.getTitle() + " - " + this.sheetName;
    }

}
