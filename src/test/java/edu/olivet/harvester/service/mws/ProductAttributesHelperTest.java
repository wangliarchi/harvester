package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.products.model.Product;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

@Guice
public class ProductAttributesHelperTest extends BaseTest {

    private ProductClient productClient;
    private Product product;

    @Inject private ProductAttributesHelper productAttributesHelper;

    @BeforeClass
    public void init() {
        productClient = new ProductClient(){
            @Override
            public Product getProductByASIN(Country country, String asin) {
                File localProductXMLFile = new File(TEST_DATA_ROOT+File.separator+"asin-"+asin+".xml");
                String xmlFragment = Tools.readFileToString(localProductXMLFile);

                Product product = MWSUtils.buildMwsObject(xmlFragment, Product.class);
                return product;
            }
        };

    }

    @Test
    public void testGetProductGroup() throws Exception {

        Map<String, String> productGroupsMap = new HashMap<>();
        productGroupsMap.put("0310435641","Book");
        productGroupsMap.put("B000FAGGWQ","DVD");
        productGroupsMap.put("B01G47RQ5O","Music");
        productGroupsMap.put("B01N5L2SU4","Book");
        productGroupsMap.put("B015LYC2S2","Major Appliances");

        productGroupsMap.forEach((asin,group)->{
            product = productClient.getProductByASIN(Country.US,asin);
            assertEquals(productAttributesHelper.getProductGroup(product), group);
        });




    }

}