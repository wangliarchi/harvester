package edu.olivet.harvester.common;


import com.alibaba.fastjson.JSON;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import edu.olivet.deploy.Language;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 单元测试基类，提供基本的订单样例，系统、程序参数样本
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu>OURnD</a> Sep 23, 2017 1:48:45 PM
 */
@Guice(modules = {MockDateModule.class, MockDBModule.class, BaseTest.AppScriptModule.class})
public class BaseTest {

    protected static String basePath;
    private static final String TEST_RES_ROOT = "src/test/resources";
    private static final String TEST_CONFIG = "harvester-test.json";
    protected static final String TEST_DATA_ROOT = TEST_RES_ROOT + "/data4test";
    protected static String testConfigFilePath;

    protected static Order order;

    static class FakeAppScript extends AppScript {
        @Override
        public Spreadsheet reloadSpreadsheet(String spreadId) {
            File localJsonFile = new File(BaseTest.TEST_DATA_ROOT + File.separator + "spreadsheet-" + spreadId + ".json");

            if (!localJsonFile.exists()) {
                return super.reloadSpreadsheet(spreadId);
            }

            Spreadsheet spreadsheet = JSON.parseObject(Tools.readFileToString(localJsonFile), Spreadsheet.class);

            spreadsheet = afterSpreadsheetLoaded(spreadsheet);

            return spreadsheet;

        }

        @Override
        public Spreadsheet afterSpreadsheetLoaded(Spreadsheet spreadsheet) {
            spreadsheet.setSpreadsheetCountry(Settings.load(testConfigFilePath).getSpreadsheetCountry(spreadsheet.getSpreadsheetId()));
            spreadsheet.setSpreadsheetType(Settings.load(testConfigFilePath).getSpreadsheetType(spreadsheet.getSpreadsheetId()));

            return spreadsheet;
        }

        @Override
        public Spreadsheet getSpreadsheet(String spreadId) {
            return reloadSpreadsheet(spreadId);
        }

        @Override
        public List<Order> readOrders(String spreadId, String sheetName) {
            String localJSONFilePath = BaseTest.TEST_DATA_ROOT + File.separator +
                "spreadsheet-data-" + spreadId + "-" + sheetName.replaceAll("/", "") + ".json";
            File localJsonFile = new File(localJSONFilePath);

            if (!localJsonFile.exists()) {
                return super.readOrders(spreadId, sheetName);
            }

            String json = Tools.readFileToString(localJsonFile);
            return this.parse(json);
        }
    }

    static class AppScriptModule extends AbstractModule {

        @Override
        protected void configure() {
            this.bind(AppScript.class).to(FakeAppScript.class);
        }
    }

    @Inject
    protected AppScript appScript;

    @Inject
    protected DBManager dbManager;

    @BeforeClass
    public void setUp() throws IOException, BusinessException {

        UIText.setLocale(Language.EN_US);
        basePath = new File(BaseTest.class.getResource("/").getFile()).getAbsolutePath() + File.separator;
        testConfigFilePath = basePath + "conf" + File.separator + BaseTest.TEST_CONFIG;
    }


    /**
     * 准备单元测试所需要用到的order
     */
    public static Order prepareOrder() {
        Order order = new Order();

        order.row = 1;
        order.status = "n";
        order.order_id = "002-1578027-1397838";
        order.recipient_name = "Nicholas Adamo";
        order.purchase_date = "10/24/2014 21:00:00";
        order.sku_address = "https://sellercentral.amazon.com/myi/search/OpenListingsSummary?keyword=new18140915a160118";
        order.sku = "new18140915a160118";
        order.price = "14.48";
        order.quantity_purchased = "1";
        order.shipping_fee = "16.95";
        order.ship_state = "NSW";
        order.isbn_address = "http://www.amazon.com/dp/0545521378";
        order.isbn = "0545521378";
        order.seller = "AP";
        order.seller_id = "";
        order.seller_price = "9.94";
        order.url = "/";
        order.condition = "New";
        order.character = "AP";
        order.remark = "无Remark";
        order.reference = "1.018";
        order.code = "29";
        order.profit = "7.488";
        order.item_name = "[ NOWHERE TO RUN (39 CLUES: UNSTOPPABLE #01) ] By Watson. Jude ( Author) 2013...";
        order.ship_address_1 = "Ernst  Young";
        order.ship_address_2 = "Level 45. 680 George Street";
        order.ship_city = "Sydney";
        order.ship_zip = "2000";
        order.ship_phone_number = "123456";
        order.cost = "20.42";
        order.order_number = "102-0780405-2545043";
        order.account = "joshjohnsonsf007@gmail.com";
        order.last_code = "10.48";
        order.setShip_country("Australia");
        order.sales_chanel = "Amazon.com";

        return order;
    }


}
