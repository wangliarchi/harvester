package edu.olivet.harvester.service;

import com.ECS.client.jax.Item;
import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/13/17 4:17 PM
 */
public class AmazonProductApiTest {
    @Test
    public void testItemLookup() throws Exception {
        List<String> asins = Lists.newArrayList("0310437105","1563207214");
        HashMap<String, Item> items = AmazonProductApi.getInstance().itemLookup(asins);
        System.out.println(items);
    }

}