package edu.olivet.harvester.utils.common;

import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Now;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class DatetimeHelperTest extends BaseTest {
    @Inject Now now;

    @Test
    public void parseEdd() {
        now.set(Dates.parseDate("2018-02-02"));
        assertEquals(DatetimeHelper.parseEdd("Arrives between February 12-21.", Country.US, now.get()), Dates.parseDate("2018-02-21"));
    }

}