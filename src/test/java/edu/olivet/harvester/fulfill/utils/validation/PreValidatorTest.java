package edu.olivet.harvester.fulfill.utils.validation;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.fulfill.model.ItemCompareResult;
import edu.olivet.harvester.model.Order;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/13/17 10:26 AM
 */
public class PreValidatorTest extends BaseTest {


    @Test
    public void testCompareItemNames4Orders() throws Exception {

        final String SPREADSHEET_ID = "1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI";
        List<Order> orders = appScript.readOrders(SPREADSHEET_ID, "11/28");
        long start = System.currentTimeMillis();
        List<ItemCompareResult> results = PreValidator.compareItemNames4Orders(orders);
        String summary = "Checked " + orders.size() + " items, took " + Strings.formatElapsedTime(start);
        System.out.println(summary);

    }

}