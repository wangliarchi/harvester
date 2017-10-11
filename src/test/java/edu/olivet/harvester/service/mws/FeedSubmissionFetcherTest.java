package edu.olivet.harvester.service.mws;

import com.amazonaws.mws.model.FeedSubmissionInfo;
import com.amazonaws.mws.model.StatusList;
import com.amazonaws.mws.model.TypeList;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.mock.MockDBModule;
import edu.olivet.foundations.mock.MockDateModule;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/7/17 2:20 AM
 */
@Guice(modules = {MockDateModule.class, MockDBModule.class})
public class FeedSubmissionFetcherTest {

    @Inject
    FeedSubmissionFetcher feedSubmissionFetcher;

    @Test
    public void testGetActiveShipmentConfirmationSubmissionList() throws Exception {
    }

    @Test
    public void testGetFeedSubmissionList() throws Exception {
        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(Country.US).getMwsCredential();
        //TypeList feedTypeList = new TypeList(Collections.singletonList("_POST_FLAT_FILE_FULFILLMENT_DATA_"));
        TypeList feedTypeList = new TypeList();
        StatusList feedProcessingStatusList = new StatusList(Stream.of("_IN_PROGRESS_", "_SUBMITTED_").collect(Collectors.toList()));

        List<FeedSubmissionInfo> result
                = feedSubmissionFetcher.getFeedSubmissionList(credential, feedTypeList, feedProcessingStatusList);

        System.out.println(result.toString());

    }

}