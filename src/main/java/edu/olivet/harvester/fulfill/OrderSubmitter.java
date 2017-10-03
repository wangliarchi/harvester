package edu.olivet.harvester.fulfill;


import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
