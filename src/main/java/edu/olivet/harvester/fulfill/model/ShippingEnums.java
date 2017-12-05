package edu.olivet.harvester.fulfill.model;

import edu.olivet.foundations.utils.Strings;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/1/17 1:38 PM
 */
public class ShippingEnums {

    public enum ShippingSpeed {
        Standard("Standard,Estándar,Rapide", "S"),
        Expedited("Expedited,Priority,Two-Day,Same-Day,One-Day,Express,Éclair", "E");

        private String keywords;
        private String code;

        public String getKeywords() {
            return keywords;
        }

        public String getCode() {
            return code;
        }

        ShippingSpeed(String keywords, String code) {
            this.keywords = keywords;
            this.code = code;
        }

        public static ShippingSpeed get(String description) {
            for (ShippingSpeed shippingSpeed : ShippingSpeed.values()) {
                String[] keyworkds = StringUtils.split(shippingSpeed.getKeywords(), ",");
                if (Strings.containsAnyIgnoreCase(description, keyworkds)) {
                    return shippingSpeed;
                }

            }

            return null;
        }
    }


    public enum ShippingType {
        International("I"),
        Domestic("D");

        private String code;

        public String getCode() {
            return code;
        }

        ShippingType(String code) {
            this.code = code;
        }

    }


}
