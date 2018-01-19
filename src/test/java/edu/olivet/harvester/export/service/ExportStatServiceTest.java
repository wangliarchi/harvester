package edu.olivet.harvester.export.service;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/18/17 9:49 AM
 */
public class ExportStatServiceTest extends BaseTest {

    @Inject private ExportStatService exportStatService;
    @Test
    public void testLastOrderDate() throws Exception {
        String spreadsheetId = "1t1iEDNrokcqjE7cTEuYW07Egm6By2CNsMuog9TK1LhI";
        Date date = exportStatService.lastOrderDate(spreadsheetId);
    }

}