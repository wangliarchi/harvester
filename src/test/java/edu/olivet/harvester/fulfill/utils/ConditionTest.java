package edu.olivet.harvester.fulfill.utils;

import edu.olivet.harvester.fulfill.utils.ConditionUtils.Condition;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ConditionTest {


    @Test
    public void parseFromText() {
        assertEquals(Condition.parseFromText("New"),Condition.New);

        assertEquals(Condition.parseFromText("Used"),Condition.Used);
        assertEquals(Condition.parseFromText("Used - Like New"),Condition.UsedLikeNew);
        assertEquals(Condition.parseFromText("Used - Very Good"),Condition.UsedVeryGood);
        assertEquals(Condition.parseFromText("Used - Good"),Condition.UsedGood);
        assertEquals(Condition.parseFromText("Used - Acceptable"),Condition.UsedAcceptable);

        assertEquals(Condition.parseFromText("Collectible - Like New"),Condition.CollectibleLikeNew);
        assertEquals(Condition.parseFromText("Collectible - Very Good"),Condition.CollectibleVeryGood);
        assertEquals(Condition.parseFromText("Collectible - Good"),Condition.CollectibleGood);
        assertEquals(Condition.parseFromText("Collectible - Acceptable"),Condition.CollectibleAcceptable);
    }

    @Test
    public void parseFromTextDE() {
        assertEquals(Condition.parseFromText("Neu"),Condition.New);
        assertEquals(Condition.parseFromText("Gebraucht"),Condition.Used);
        assertEquals(Condition.parseFromText("Gebraucht - Wie neu"),Condition.UsedLikeNew);
        assertEquals(Condition.parseFromText("Gebraucht - Sehr gut"),Condition.UsedVeryGood);
        assertEquals(Condition.parseFromText("Gebraucht - Gut"),Condition.UsedGood);
        assertEquals(Condition.parseFromText("Gebraucht - Akzeptabel"),Condition.UsedAcceptable);
    }

    @Test
    public void parseFromTextFR() {
        assertEquals(Condition.parseFromText("Neuf"),Condition.New);
        assertEquals(Condition.parseFromText("D'occasion"),Condition.Used);
        assertEquals(Condition.parseFromText("D'occasion - Comme neuf"),Condition.UsedLikeNew);
        assertEquals(Condition.parseFromText("D'occasion - Très bon"),Condition.UsedVeryGood);
        assertEquals(Condition.parseFromText("D'occasion - Bon"),Condition.UsedGood);
        assertEquals(Condition.parseFromText("D'occasion - Acceptable"),Condition.UsedAcceptable);
    }

    @Test
    public void parseFromTextES() {
        assertEquals(Condition.parseFromText("Nuevo"),Condition.New);
        assertEquals(Condition.parseFromText("De 2ª mano"),Condition.Used);
        assertEquals(Condition.parseFromText("De 2ª mano - Como nuevo"),Condition.UsedLikeNew);
        assertEquals(Condition.parseFromText("De 2ª mano - Muy bueno"),Condition.UsedVeryGood);
        assertEquals(Condition.parseFromText("De 2ª mano - Bueno"),Condition.UsedGood);
        assertEquals(Condition.parseFromText("De 2ª mano - Aceptable"),Condition.UsedAcceptable);
    }

    @Test
    public void parseFromTextIT() {
        assertEquals(Condition.parseFromText("Nuovo"),Condition.New);
        assertEquals(Condition.parseFromText("Usato"),Condition.Used);
        assertEquals(Condition.parseFromText("Usato - Come nuovo"),Condition.UsedLikeNew);
        assertEquals(Condition.parseFromText("Usato - Ottime condizioni"),Condition.UsedVeryGood);
        assertEquals(Condition.parseFromText("Usato - Buone condizioni"),Condition.UsedGood);
        assertEquals(Condition.parseFromText("Usato - Condizioni accettabili"),Condition.UsedAcceptable);
    }
    //

}