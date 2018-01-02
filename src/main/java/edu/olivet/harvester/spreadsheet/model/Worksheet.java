package edu.olivet.harvester.spreadsheet.model;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.utils.ServiceUtils;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.time.DateUtils;

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
        ZoneId zoneId = ServiceUtils.getTimeZone(spreadsheet.getSpreadsheetCountry()).toZoneId();

        Date sheetDateFromName = Dates.parseDateOfGoogleSheet(sheetName);
        sheetDateFromName = DateUtils.addHours(sheetDateFromName, 7);
        LocalDate localDate = sheetDateFromName.toInstant().atZone(zoneId).toLocalDate();

        DateTimeFormatter feedDf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return feedDf.format(localDate);

    }

    public String toString() {
        return this.spreadsheet.getTitle() + " - " + this.sheetName;
    }

}
