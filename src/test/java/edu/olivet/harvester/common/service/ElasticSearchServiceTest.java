package edu.olivet.harvester.common.service;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ElasticSearchServiceTest extends BaseTest {

    @Inject private ElasticSearchService elasticSearchService;
    @Test
    public void searchISBN() {
        assertEquals(elasticSearchService.searchISBN("B005H75W9A"),"0801062756");
    }

}