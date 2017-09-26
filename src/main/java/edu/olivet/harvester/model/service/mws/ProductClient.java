package edu.olivet.harvester.model.service.mws;

import com.amazonservices.mws.products.MarketplaceWebServiceProductsClient;
import com.amazonservices.mws.products.MarketplaceWebServiceProductsConfig;
import com.amazonservices.mws.products.model.GetMatchingProductRequest;
import com.amazonservices.mws.products.model.Product;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.ProductFetcher;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ProductClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductClient.class);


    private MarketWebServiceIdentity credential;


    /**
     * Returns a list of products and their attributes, based on a list of ASIN values..
     * https://docs.developer.amazonservices.com/en_UK/products/Products_GetMatchingProduct.html
     *
     * including PurchaseDate, OrderStatus, FulfillmentChannel, and LastUpdateDate.
     * @param country
     * @param asins
     */

    @Inject
    private ProductFetcher productFetcher;

    public  Product getProductsByASIN(Country country, String asin) {
        List<String> asins = new ArrayList<String>();
        asins.add(asin);

        List<Product> products =  getProductsByASINs(country,asins);

        if(products.isEmpty()) {
            throw  new BusinessException("No product info returned from MWS Product API for asin "+asin);
        }

        Product product = products.get(0);

        if(product == null) {
            throw new BusinessException("No product info returned from MWS Product API for asin "+asin);
        }

        return product;

    }
    public  List<Product> getProductsByASINs(Country country, List<String> asins) {

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        List<Product> result = productFetcher.read(asins,credential);




        return result;

    }
}
