package edu.olivet.harvester.service.mws;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.amazonservices.mws.products.model.AttributeSetList;
import com.amazonservices.mws.products.model.Product;
import edu.olivet.foundations.utils.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProductAttributesHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAttributesHelper.class);

    private static JSONObject parse(Product product) {
        JSONObject jsonProduct = new JSONObject();

        if (product.isSetAttributeSets()) {
            AttributeSetList attributeSetList = product.getAttributeSets();

            for (Object obj : attributeSetList.getAny()) {
                Node attribute = (Node) obj;
                NodeList nodeList = attribute.getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String nodeName = nodeList.item(i).getNodeName();
                    nodeName = nodeName.replaceFirst("ns2:", "");
                    Node myNode = nodeList.item(i);

                    try {
                        jsonProduct.put(nodeName, nodeList.item(i).getTextContent());
                    } catch (JSONException e) {
                        //ignore
                    }
                }
            }
        }

        return jsonProduct;
    }


    public static String getProductGroup(Product product) {
        try {
            return ProductAttributesHelper.parse(product).get("ProductGroup").toString();
        } catch (JSONException e) {
            throw new BusinessException("No product group information returned from mws product api.");
        }

    }
}
