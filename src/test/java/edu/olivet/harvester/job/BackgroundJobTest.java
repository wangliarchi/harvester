package edu.olivet.harvester.job;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BackgroundJobTest {
    @Test
    public void testGetCron() throws Exception {
        int counter = 0;
        while (counter < 100) {
            System.out.println(BackgroundJob.ShipmentConfirmation.getCron());
            counter++;
        }


    }

}