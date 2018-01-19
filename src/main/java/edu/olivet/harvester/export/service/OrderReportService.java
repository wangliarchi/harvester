package edu.olivet.harvester.export.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums.Status;
import edu.olivet.harvester.spreadsheet.service.OrderHelper;
import edu.olivet.harvester.utils.ServiceUtils;
import edu.olivet.harvester.utils.common.DateFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/4/2018 12:40 PM
 */
public class OrderReportService {

    public void readReportFile(String path) {
        File file = new File(getReportDirectory() + "/" + path);

        List<String> headers = new ArrayList<>();
        for (String line : Tools.readLines(file)) {
            if (StringUtils.contains(line, "order-id")) {
                headers = Arrays.asList(line.split("\t"));
            } else {
                Order order = new Order();
                String[] values = line.split("\t");


                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    String column = headers.get(i);
                    try {
                        orderHelper.setColumnValue(column, value, order);
                    } catch (Exception e) {
                        //
                    }

                    if ("item-price".equalsIgnoreCase(column)) {
                        order.price = value;
                    }
                    if ("shipping-price".equalsIgnoreCase(column)) {
                        order.shipping_fee = value;
                    }

                    if ("asin".equalsIgnoreCase(column)) {
                        order.sku_address = value;
                    }

                    if ("product-name".equalsIgnoreCase(column)) {
                        order.item_name = value;
                    }

                    if ("ship-postal-code".equalsIgnoreCase(column)) {
                        order.ship_zip = value;
                    }

                    if ("ship-phone-number".equalsIgnoreCase(column)) {
                        order.ship_phone_number = value;
                    }

                    if ("sales-channel".equalsIgnoreCase(column)) {
                        order.sales_chanel = value;
                    }

                    if ("ship-service-name".equalsIgnoreCase(column)) {
                        order.shipping_service = value;
                    }

                    if ("earliest-ship-date".equalsIgnoreCase(column)) {
                        order.expected_ship_date = value;
                    }
                    if ("latest-ship-date".equalsIgnoreCase(column)) {
                        //noinspection StringConcatenationInLoop
                        order.expected_ship_date += " - " + value;
                    }

                    if ("earliest-delivery-date".equalsIgnoreCase(column)) {
                        order.estimated_delivery_date = value;
                    }
                    if ("latest-delivery-date".equalsIgnoreCase(column)) {
                        //noinspection StringConcatenationInLoop
                        order.estimated_delivery_date += " - " + value;
                    }



                }
                order = fixOrder(order);
                System.out.println(order);
            }
        }

    }


    private Order fixOrder(Order order) {

        Country salesChanelCountry = Country.fromSalesChanel(order.sales_chanel);

        order.status = Status.Initial.value();
        order.sku_address = salesChanelCountry.baseUrl() + "/dp/" + order.sku_address;
        order.seller = order.seller_id = order.seller_price = StringUtils.EMPTY;
        order.url = StringUtils.EMPTY;
        order.condition = StringUtils.EMPTY;
        order.character = order.remark = order.reference = order.code = order.profit = StringUtils.EMPTY;
        order.cost = order.order_number = order.account = order.last_code = StringUtils.EMPTY;
        order.sid = StringUtils.EMPTY;


        //noinspection CheckStyle
        FastDateFormat ORDER_DATE_FORMAT = FastDateFormat.getInstance(DateFormat.DATE_TIME_STR.pattern(),
                ServiceUtils.getTimeZone(salesChanelCountry));
        //noinspection CheckStyle
        FastDateFormat SHIP_DATE_FORMAT = FastDateFormat.getInstance(DateFormat.FULL_DATE.pattern(),
                ServiceUtils.getTimeZone(salesChanelCountry));


        return order;
    }

    @Inject private OrderHelper orderHelper;


    private String getReportDirectory() {
        return Directory.APP_DATA + File.separator + "reports/orders";
    }


    public static void main(String[] args) {
        OrderReportService orderReportService = ApplicationContext.getBean(OrderReportService.class);
        orderReportService.readReportFile("8976688073017535.txt");
    }
}
