package edu.olivet.harvester.common.service.mws;

import com.amazonaws.mws.MarketplaceWebServiceClient;
import com.amazonaws.mws.MarketplaceWebServiceException;
import com.amazonaws.mws.model.*;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.FeedUploader;
import edu.olivet.foundations.amazon.MWSUtils;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.utils.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/7/17 1:31 AM
 */
public class FeedSubmissionFetcher extends FeedUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedSubmissionFetcher.class);


    public List<FeedSubmissionInfo> getActiveShipmentConfirmationSubmissionList(Country country) {

        MarketWebServiceIdentity credential = Settings.load().getConfigByCountry(country).getMwsCredential();
        TypeList feedTypeList = new TypeList(Collections.singletonList("_POST_FLAT_FILE_FULFILLMENT_DATA_"));
        StatusList feedProcessingStatusList = new StatusList(Stream.of("_IN_PROGRESS_", "_SUBMITTED_").collect(Collectors.toList()));

        return getFeedSubmissionList(credential, feedTypeList, feedProcessingStatusList);

    }

    public List<FeedSubmissionInfo> getFeedSubmissionList(MarketWebServiceIdentity credential,
                                                          TypeList feedTypeList, StatusList feedProcessingStatusList) {
        GetFeedSubmissionListRequest request = new GetFeedSubmissionListRequest();
        request.setMerchant(credential.getSellerId());

        if (feedProcessingStatusList.getStatus().size() > 0) {
            request.setFeedProcessingStatusList(feedProcessingStatusList);
        }

        if (feedTypeList.getType().size() > 0) {
            request.setFeedTypeList(feedTypeList);
        }


        int times = 0, maxTimes = 3, waitTime = 1;
        MarketplaceWebServiceClient client = MWSUtils.service(credential);
        Country country = Country.fromMarketplaceId(credential.getMarketPlaceId());
        while (times <= maxTimes) {
            try {
                GetFeedSubmissionListResponse response = client.getFeedSubmissionList(request);
                if (response.isSetGetFeedSubmissionListResult()) {
                    GetFeedSubmissionListResult feedSubmissionListResult = response.getGetFeedSubmissionListResult();

                    return feedSubmissionListResult.getFeedSubmissionInfoList();

                }
            } catch (MarketplaceWebServiceException e) {
                if (MWSUtils.isFatal(e)) {
                    throw new BusinessException(e.getXML());
                }

                times++;
                LOGGER.warn("GetFeedSubmissionList 无执行结果, 尝试第{}次等待{}分钟: {}", country.label(), times, waitTime, e);
                Tools.sleep(waitTime, TimeUnit.MINUTES);
            }
        }

        throw new BusinessException("Failed to get feed submission list");
    }
}
