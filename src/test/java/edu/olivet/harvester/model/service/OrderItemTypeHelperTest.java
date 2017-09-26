package edu.olivet.harvester.model.service;

import com.google.inject.Inject;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.OrderEnums;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Guice
public class OrderItemTypeHelperTest {

    @Inject private OrderItemTypeHelper helper;

    @Test(expectedExceptions = BusinessException.class)
    public void testNoValidASIN() {
        Order order = BaseTest.prepareOrder();
        order.sku_address = "https://sellercentral.amazon.com/myi/search/OpenListingsSummary?keyword=new18140915a160118";
        order.isbn = "";

        helper.getItemTypeByMWSAPI(order);

    }


    @Test
    public void testGetItemType() {
        Order order = BaseTest.prepareOrder();

        //niv bible
        order.isbn = "0310435641";
        order.sku = "";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.BOOK);


        //B01N5L2SU4 JiuXMZSMusbkAug22-2017-C657529 book
        order.isbn = "B01N5L2SU4";
        order.sku = "JiuXMZSMusbkAug22-2017-C657529";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.BOOK);


        //B008KTVD90 usedAcptc-c0311201713941 -> book
        order.isbn = "B008KTVD90";
        order.sku = "usedAcptc-c0311201713941";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.BOOK);

        //B01G47RQ5O  New700uk160527uscd-g85725 cd -> book
        order.sku = "New700uk160527uscd-g85725";
        order.isbn = "B01G47RQ5O";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.BOOK);

        //B000FAGGWQ NEW-71-USDvd06162016-G045822 dvd->book
        order.sku = "NEW-71-USDvd06162016-G045822";
        order.isbn = "B000FAGGWQ";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.BOOK);



        //B015LYC2S2 XinXMZukkitchenJune16-2017-P0021687 kitchen -> product
        order.sku = "XinXMZukkitchenJune16-2017-P0021687";
        order.isbn = "B015LYC2S2";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.PRODUCT);



        //B0094KQG00 new701CAusubanP160805ZD-P030969
        order.sku = "new701CAusubanP160805ZD-P030969";
        order.isbn = "B0094KQG00";
        assertEquals(helper.getItemType(order), OrderEnums.OrderItemType.PRODUCT);

    }



    @Test (expectedExceptions = BusinessException.class)
    public void testGetItemTypeByMWSAPINotValidASIN() {
        Order order = BaseTest.prepareOrder();

        //Invalid ASIN
        order.isbn = "0310435641A";

        helper.getItemTypeByMWSAPI(order);
    }

    @Test
    public void testGetItemTypeByMWSAPI() {
        Order order = BaseTest.prepareOrder();

        //niv bible
        order.isbn = "0310435641";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.BOOK);


        //B01N5L2SU4 JiuXMZSMusbkAug22-2017-C657529 book
        order.isbn = "B01N5L2SU4";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.BOOK);


        //B008KTVD90 usedAcptc-c0311201713941 -> book
        order.isbn = "B008KTVD90";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.BOOK);

        //B01G47RQ5O  New700uk160527uscd-g85725 cd -> book
        order.isbn = "B01G47RQ5O";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.BOOK);

        //B000FAGGWQ NEW-71-USDvd06162016-G045822 dvd->book
        order.isbn = "B000FAGGWQ";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.BOOK);

        //B015LYC2S2 XinXMZukkitchenJune16-2017-P0021687 kitchen -> product
        order.isbn = "B015LYC2S2";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.PRODUCT);



        //B0094KQG00 new701CAusubanP160805ZD-P030969
        order.isbn = "B0094KQG00";
        assertEquals(helper.getItemTypeByMWSAPI(order), OrderEnums.OrderItemType.PRODUCT);

    }

    @Test
    public  void  testGetItemTypeBySku() {
        Order order = BaseTest.prepareOrder();

        //B01N5L2SU4 book
        order.sku = "JiuXMZSMusbkAug22-2017-C657529";
        assertEquals(helper.getItemTypeBySku(order), OrderEnums.OrderItemType.BOOK);

        //B015LYC2S2 kitchen -> product
        order.sku = "XinXMZukkitchenJune16-2017-P0021687";
        assertEquals(helper.getItemTypeBySku(order), OrderEnums.OrderItemType.PRODUCT);

        //B01G47RQ5O cd -> book
        order.sku = "New700uk160527uscd-g85725";
        assertEquals(helper.getItemTypeBySku(order), OrderEnums.OrderItemType.BOOK);

        //B0094KQG00
        order.sku = "new701CAusubanP160805ZD-P030969";
        assertEquals(helper.getItemTypeBySku(order), OrderEnums.OrderItemType.PRODUCT);

        //B008KTVD90 -> book
        order.sku = "usedAcptc-c0311201713941";
        assertEquals(helper.getItemTypeBySku(order), OrderEnums.OrderItemType.BOOK);

    }


}