package edu.olivet.harvester.service.mws;

import com.amazonservices.mws.products.model.Product;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.ProductFetcher;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ProductClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductClient.class);


    private MarketWebServiceIdentity credential;


    /**
     * <pre>Returns a list of products and their attributes, based on a list of ASIN values..
     * including PurchaseDate, OrderNotFulfilled, FulfillmentChannel, and LastUpdateDate.
     * https://docs.developer.amazonservices.com/en_UK/products/Products_GetMatchingProduct.html
     * </pre>
     */
    public Product getProductByASIN(Country country, String asin) {
        List<String> asins = new ArrayList<>();
        asins.add(asin);

        List<Product> products = getProductsByASINs(country, asins);

        if (products.isEmpty()) {
            throw new BusinessException("No product info returned from MWS Product API for asin " + asin);
        }

        Product product = products.get(0);

        if (product == null) {
            throw new BusinessException("No product info returned from MWS Product API for asin " + asin);
        }

        return product;

    }

    @Inject
    private ProductFetcher productFetcher;

    public List<Product> getProductsByASINs(Country country, List<String> asins) {

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();

        return productFetcher.read(asins, credential);
    }
}
