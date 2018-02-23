package edu.olivet.harvester.hunt.model;

import edu.olivet.harvester.hunt.model.SellerEnums.StockStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StockStatusTest {
    @Test
    public void parseFromText() {
        String text = "Derzeit nicht auf Lager. Bestellen Sie jetzt und wir liefern, sobald der Artikel verfügbar ist. Sie erhalten von uns eine E-Mail mit dem voraussichtlichen Lieferdatum, sobald uns diese Information vorliegt. Ihr Konto wird erst dann belastet, wenn wir den Artikel verschicken.";
        assertEquals(StockStatus.parseFromText(text),StockStatus.OutOfStock);
    }

}