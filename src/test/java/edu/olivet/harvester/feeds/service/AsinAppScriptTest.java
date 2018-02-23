package edu.olivet.harvester.feeds.service;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AsinAppScriptTest extends BaseTest {


    @Inject AsinAppScript asinAppScript;

    @Test
    public void getAsinInventoryLoaderSync() {
        System.out.println(asinAppScript.getAsinInventoryLoaderSync());
    }

}