package edu.olivet.harvester.fulfill;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.teamdev.jxbrowser.chromium.SavePageType;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.harvester.model.Order;
import edu.olivet.harvester.model.Spreadsheet;
import edu.olivet.harvester.utils.Settings;
import edu.olivet.harvester.utils.Settings.Configuration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order station prototype entry
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Singleton
public class OrderSubmitter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSubmitter.class);
    private static final String SPREAD_ID = "1LEU2GXvfEXEkbQS42FeUPPLkpbI4iBqU9OWDV13KsO8";


    private void execute() {

    }

    public static void main(String[] args) {
        UITools.setTheme();
        ApplicationContext.getBean(OrderSubmitter.class).execute();
    }

}
