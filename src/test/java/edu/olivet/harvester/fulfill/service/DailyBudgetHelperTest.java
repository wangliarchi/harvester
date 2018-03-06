package edu.olivet.harvester.fulfill.service;

import com.google.inject.Inject;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.foundations.utils.Now;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class DailyBudgetHelperTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyBudgetHelperTest.class);
    @Inject DailyBudgetHelper dailyBudgetHelper;
    @Inject Now now;

    @Test
    public void addSpending() {

    }

}