package edu.olivet.harvester.common.model;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class RemarkTest extends BaseTest {
    @Inject AppScript appScript;

    @Test
    public void removeFailedRemark() {
        String spreadsheetId = "1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI";
        String sheetName = "03/09";
        List<Order> orders = appScript.readOrders(spreadsheetId, sheetName);
        //orders.removeIf(it -> !it.fulfilled());

        for (Order order : orders) {
            if (StringUtils.isBlank(order.remark)) {
                continue;
            }
            System.out.println("Row " + order.row);
            System.out.println("Original Remark: " + order.remark);
            System.out.println("Updated Remark: " + Remark.removeFailedRemark(order.remark));
            System.out.println("\n");
        }

    }

}