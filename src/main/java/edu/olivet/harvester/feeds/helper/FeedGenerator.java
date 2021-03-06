package edu.olivet.harvester.feeds.helper;

import com.amazonservices.mws.orders._2013_09_01.model.OrderItem;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.model.OrderEnums;
import edu.olivet.harvester.common.model.OrderEnums.OrderItemType;
import edu.olivet.harvester.feeds.model.InventoryUpdateRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class FeedGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedGenerator.class);

    /**
     * 批量上传文件类型，通常包括订单运输确认、库存更新(下架、删点、补点)等等
     *
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Aug 14, 2015 10:01:04 AM
     */
    public enum BatchFileType {
        /**
         * 常规删点文件
         */
        ListingDeletion("InvLoaderData", "sku\tadd-delete", "_POST_FLAT_FILE_INVLOADER_DATA_"),
        /**
         * 上传跟卖Listing，需要基于一个已经存在的有效ASIN，注意，不同于<strong>造点</strong>
         */
        ListingUpload("InvLoaderData", "sku\tproduct-id\tproduct-id-type\tprice\t" +
                "minimum-seller-allowed-price\tmaximum-seller-allowed-price\titem-condition\tquantity", "_POST_FLAT_FILE_INVLOADER_DATA_"),
        /**
         * 订单Tracking文件
         */
        ShippingConfirmation("ShippingConfirmation", "order-id\tcarrier-code\tcarrier-name\tship-date",
                "_POST_FLAT_FILE_FULFILLMENT_DATA_"),
        /**
         * Refund self order 文件
         */
        SelfOrderRefund("SelfOrderRefund", "order-id\torder-item-id\tadjustment-reason-code\tcurrency\titem-price-adj", "_POST_FLAT_FILE_PAYMENT_ADJUSTMENT_DATA_"),

        /**
         * 更改库存数量文件
         */
        ReQuantity("PriceAndQty", "sku\tquantity\thandling-time", "_POST_FLAT_FILE_PRICEANDQUANTITYONLY_UPDATE_DATA_"),
        /**
         * 更改货品价格文件
         */
        RePrice("PriceAndQty", "sku\tprice", "_POST_FLAT_FILE_PRICEANDQUANTITYONLY_UPDATE_DATA_"),
        /**
         * 同时更改库存数量、价格文件
         */
        PriceAndQuantity("PriceAndQty", "sku\tprice\tquantity", "_POST_FLAT_FILE_PRICEANDQUANTITYONLY_UPDATE_DATA_"),
        /**
         * 印度卖场删点（较为特殊）
         */
        INListingDeletion("PriceAndQty", "sku\tupdate_delete", "_POST_FLAT_FILE_INVLOADER_DATA_");

        private final String uploadTypeCode;
        private final String headers;
        private final String feedType;

        BatchFileType(String uploadTypeCode, String headers, String feedType) {
            this.uploadTypeCode = uploadTypeCode;
            this.headers = headers;
            this.feedType = feedType;
        }

        public String feedType() {
            return this.feedType;
        }

        public String uploadTypeCode() {
            return uploadTypeCode;
        }

        public String headers() {
            return headers;
        }

        public String label() {
            return UIText.label("label.batchfile." + this.uploadTypeCode.toLowerCase());
        }
    }


    //
    public File generateQtyUpdateFeedFromRows(List<InventoryUpdateRecord> records, Country country, OrderEnums.OrderItemType type) {
        List<String> contents = new ArrayList<>();
        contents.add(BatchFileType.ReQuantity.headers());

        //skuquantityhandling-time
        Set<String> skus = new HashSet<>();
        for (InventoryUpdateRecord row : records) {
            if (skus.contains(row.getSku())) {
                continue;
            }
            contents.add(String.format("%s\t%s\t%s", row.getSku(), row.getQty(), row.getLeadTime(country)));
            skus.add(row.getSku());
        }

        File file;
        try {
            file = this.initReportFile(BatchFileType.ReQuantity.uploadTypeCode, country, type);
        } catch (Exception e) {
            LOGGER.error(" ", e);
            throw new BusinessException(e);
        }
        Tools.writeLines(file, contents);
        return file;
    }


    public File generateASINRemovalFeedFromRows(List<InventoryUpdateRecord> records, Country country, OrderEnums.OrderItemType type) {

        Set<String> skus = records.stream().map(InventoryUpdateRecord::getSku).collect(Collectors.toSet());
        List<String> contents = new ArrayList<>(skus.size() + 1);
        contents.add(BatchFileType.ListingDeletion.headers());

        //skuadd-delete
        for (String sku : skus) {
            contents.add(String.format("%s\t%s", sku, "x"));
        }

        File file;
        try {
            file = this.initReportFile(BatchFileType.ListingDeletion.uploadTypeCode, country, type);
        } catch (Exception e) {
            LOGGER.error(" ", e);
            throw new BusinessException(e);
        }
        Tools.writeLines(file, contents);
        return file;
    }

    //
    public File generateConfirmShipmentFeedFromRows(List<String[]> orders, Country country, OrderEnums.OrderItemType type) {
        //LOGGER.info("generating order confirmation file for {}", country.name(),type.name());
        List<String> contents = new ArrayList<>(orders.size() + 1);
        contents.add(BatchFileType.ShippingConfirmation.headers());

        for (String[] row : orders) {
            contents.add(String.format("%s\t%s\t%s\t%s", row[0], row[1], row[2], row[3]));
        }

        File file;
        try {
            file = this.initReportFile(BatchFileType.ShippingConfirmation.uploadTypeCode, country, type);
        } catch (Exception e) {
            LOGGER.error(" ", e);
            throw new BusinessException(e);
        }
        Tools.writeLines(file, contents);
        return file;
    }

    public File generateSelfOrdersRefund(List<String[]> orders, Country country, OrderEnums.OrderItemType sheetType){

        List<String> contents = new ArrayList<>(orders.size() + 1);
        contents.add(BatchFileType.SelfOrderRefund.headers());

        for (String[] row : orders){
            contents.add(String.format("%s\t%s\t%s\t%s\t%s", row[0], row[1], row[2], row[3], row[4]));
        }

        File file;
        try {
            file = this.initReportFile(BatchFileType.SelfOrderRefund.uploadTypeCode, country, sheetType);
        } catch (Exception e){
            LOGGER.error(" ", e);
            throw new BusinessException(e);
        }

        Tools.writeLines(file, contents);
        return file;

    }

    /**
     * initialize feed file.
     */
    private File initReportFile(String feedType, Country country, OrderEnums.OrderItemType sheetType) {
        String filePath = country.name() + "_" + sheetType.name() + "_" + feedType + "_" + Dates.nowAsFileName() + ".txt";
        return new File(this.getFeedDirectory(), filePath);

    }


    private String getFeedDirectory() {
        return Directory.APP_DATA + File.separator + "feeds";
    }

}
