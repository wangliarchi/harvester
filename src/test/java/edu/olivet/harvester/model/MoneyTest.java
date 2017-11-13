package edu.olivet.harvester.model;

import edu.olivet.foundations.amazon.Country;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/9/17 8:31 PM
 */
public class MoneyTest {
    @Test
    public void testTromText() throws Exception {
        assertEquals(Money.fromText("$10.58", Country.US).getAmount().toPlainString(), "10.58");
        assertEquals(Money.fromText("$2,340.58", Country.US).getAmount().toPlainString(), "2340.58");
        assertEquals(Money.fromText("90,83 €",Country.FR).getAmount().toPlainString(),"90.83");
        assertEquals(Money.fromText("2.390,83 €",Country.FR).getAmount().toPlainString(),"2390.83");
        assertEquals(Money.fromText("2 390,83 €",Country.FR).getAmount().toPlainString(),"2390.83");
        assertEquals(Money.fromText("2.390,83 €",Country.DE).getAmount().toPlainString(),"2390.83");
    }

}