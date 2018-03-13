package edu.olivet.harvester.letters.service;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.TemplateHelper;
import edu.olivet.foundations.utils.TemplateHelper.EmailContent;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.letters.model.GrayEnums.GrayLetterType;
import edu.olivet.harvester.letters.model.Letter;
import edu.olivet.harvester.utils.Settings;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:57 AM
 */
public class LetterTemplateService {
    @Inject
    private TemplateHelper templateHelper;

    public Letter getLetter(Order order) {
        Map<String, Object> input = prepareInput(order);

        GrayLetterType type = GrayLetterType.valueOf(order.status.toUpperCase());
        String template = getTemplate(order, type);
        EmailContent content = templateHelper.getEmailContent(input, template);

        Letter letter = new Letter();
        letter.setSubject(content.getSubject());
        letter.setBody(content.getBody());
        letter.setType(type);
        letter.setOrder(order);
        return letter;
    }

    public String getTemplate(Order order, GrayLetterType type) {
        Country marketplaceCountry = OrderCountryUtils.getMarketplaceCountry(order);
        String template = type.name().toLowerCase();
        File file = new File(Directory.Template.path() + "/" + template + "_" + marketplaceCountry.name().toLowerCase() + ".ftl");
        if (file.exists()) {
            template = template + "_" + marketplaceCountry.name().toLowerCase();
        }
        return template;
    }

    public Map<String, Object> prepareInput(Order order) {
        Map<String, Object> input = new HashMap<>();
        BeansWrapper w = new BeansWrapper(Configuration.VERSION_2_3_23);
        TemplateModel statics = w.getStaticModels();
        input.put("statics", statics); // map is java.util.Map
        input.put("order", order);
        Country country = Settings.load().getSpreadsheetCountry(order.spreadsheetId);
        input.put("config", Settings.load().getConfigByCountry(country));

        return input;
    }

}
