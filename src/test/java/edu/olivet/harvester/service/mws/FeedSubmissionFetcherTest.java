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
        StatusList feedProcessingStatusList = new StatusList();

        List<FeedSubmissionInfo> result
                = feedSubmissionFetcher.getFeedSubmissionList(credential, feedTypeList, feedProcessingStatusList);

        StringBuilder submissions = new StringBuilder();
        result.forEach(it -> {
            submissions.append(String.format("FeedSubmissionId %s submitted at %s, current status %s \n", it.getFeedSubmissionId(),it.getSubmittedDate(),it.getFeedProcessingStatus()));
        });

        System.out.println(submissions.toString());

    }

}