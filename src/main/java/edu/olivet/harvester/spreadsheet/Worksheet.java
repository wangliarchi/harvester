package edu.olivet.harvester.spreadsheet;

import edu.olivet.foundations.utils.BusinessException;
import lombok.Data;
import lombok.Getter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class Worksheet {

    @Getter
    private String sheetName;

    @Getter
    private Spreadsheet spreadsheet;

    public  Worksheet(Spreadsheet spreadsheet, String sheetName) {
        this.spreadsheet = spreadsheet;
        this.sheetName = sheetName;
    }

    public String getOrderConfirmationDate() {
        //current worksheet date
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        Date now = new Date();
        LocalDate localDate = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        Date sheetDate = new Date();
        try {
            sheetDate = df.parse(localDate.getYear() + "/" + this.getSheetName());
            LocalDate sheetLocalDate = sheetDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            //attention - adjust year when the sheet day is 12/31 and current date is new year
            if (sheetLocalDate.isAfter(localDate)) {
                sheetDate = df.parse((localDate.getYear() - 1) + "/" + this.getSheetName());
            }

        } catch (ParseException e) {

            throw new BusinessException("Sheet with name " + this.getSheetName() + " is not valid");
        }


        DateFormat feedDf = new SimpleDateFormat("yyyy-MM-dd");

        String shipDate = feedDf.format(sheetDate);

        return shipDate;
    }
    public String toString() {
        return this.spreadsheet.getTitle() + " - " + this.sheetName;
    }
}
