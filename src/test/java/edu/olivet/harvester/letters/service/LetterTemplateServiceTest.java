package edu.olivet.harvester.letters.service;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.*;

public class LetterTemplateServiceTest extends BaseTest {
    @Inject LetterTemplateService letterTemplateService;

    @Test
    public void getSGLetterTest() {
        Order order = prepareOrder();
        order.spreadsheetId = Settings.load().listAllSpreadsheets().get(0);
        order.sales_chanel = "amazon.it";
        order.status = "sg";
        Letter letter = letterTemplateService.getLetter(order);
        System.out.println("Subject: " + letter.getSubject());
        System.out.println("\n");
        System.out.println(letter.getBody());
    }

    @Test
    public void getLWLetter() {
        Order order = prepareOrder();
        order.spreadsheetId = Settings.load().listAllSpreadsheets().get(0);
        order.status = "lw";
        Letter letter = letterTemplateService.getLetter(order);
        System.out.println(letter.getSubject());
        System.out.println(letter.getBody());
    }

    @Test
    public void getLDNLetter() {
        Order order = prepareOrder();
        order.spreadsheetId = Settings.load().listAllSpreadsheets().get(0);
        order.status = "dn";
        order.estimated_delivery_date = "2018-02-16 2018-02-26";
        Date d = org.apache.commons.lang3.time.DateUtils.addDays(order.latestEdd(),10);
        Letter letter = letterTemplateService.getLetter(order);
        System.out.println(letter.getSubject());
        System.out.println(letter.getBody());
    }

}