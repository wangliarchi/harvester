package edu.olivet.harvester.hunt.model;


import edu.olivet.harvester.hunt.model.SellerEnums.SellerType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SellerTypeTest {
    @Test
    public void getByCharacter() {
        assertEquals(SellerType.ImagePrime, SellerType.getByCharacter("tp_Pr"));
    }

}