package edu.olivet.harvester.feeds.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.amazon.MarketWebServiceIdentity;
import edu.olivet.foundations.amazon.ReportDownloader;
import edu.olivet.foundations.amazon.ReportDownloader.ReportType;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.*;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 2/21/2018 2:52 PM
 */
@Singleton
public class InventoryReportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryReportManager.class);
    private static final int BUFFER_SIZE = 5 * 1024 * 1024;
    private static final String INVENTORY_DIR_NAME = "inventory";
    private static final String REPORT_FILE_EXTENSION = ".txt";
    /**
     * 库存报表文件表头
     */
    private static final String[] HEADERS = {"sku", "asin", "price", "quantity"};

    @Setter
    private MessagePanel messagePanel = new VirtualMessagePanel();
    @Inject ReportDownloader reportDownloader;

    @Inject
    public void init() {
        messagePanel = new VirtualMessagePanel();
    }

    public void download(String zoneCode) {
        if ("EU".equalsIgnoreCase(zoneCode)) {
            for (Country country : Country.EURO) {
                download(country);
            }
        } else {
            download(Country.fromCode(zoneCode));
        }
    }

    public File download(Country country) {
        MarketWebServiceIdentity credential;

        if (country.europe()) {
            credential = Settings.load().getConfigByCountry(Country.UK).getMwsCredential();
            credential.setMarketPlaceId(country.marketPlaceId());
        } else {
            credential = Settings.load().getConfigByCountry(country).getMwsCredential();
        }

        String filePath = getInventoryFilePath(country);
        File file = new File(filePath);


        //local cache for 12 hours
        if (file.exists()) {
            try {
                Path p = Paths.get(file.getAbsolutePath());
                BasicFileAttributes attr = Files.readAttributes(p, BasicFileAttributes.class);
                if (System.currentTimeMillis() - attr.lastModifiedTime().toMillis() < 12 * 60 * 60 * 1000) {
                    return file;
                }
            } catch (Exception e) {
                //
            }
        }

        Tools.createFileIfNotExist(file);
        reportDownloader.setMessagePanel(messagePanel);

        try {
            reportDownloader.execute(ReportType.INVENTORY, credential, file);
        } catch (Exception e) {
            LOGGER.error("", e);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            throw e;
        }
        return file;
    }

    public static String getInventoryFilePath(Country country) {
        String inventoryFolderPath = getInventoryDirName(country);
        return inventoryFolderPath + File.separator + country.name() + REPORT_FILE_EXTENSION;
    }

    public static String getInventoryDirName(Country country) {
        return Directory.APP_DATA + File.separator + INVENTORY_DIR_NAME + File.separator + country.zoneCode();
    }

    /**
     * 获取库存报表文件路径
     *
     * @param country 国家
     */
    public static List<String> getInventoryFilePaths(Country country) {
        String inventoryFolderPath = getInventoryDirName(country);
        File folder = new File(inventoryFolderPath);
        if (!folder.exists()) {
            throw new BusinessException(String.format("%s对应库存文件夹%s不存在", country.label(), folder.getAbsolutePath()));
        }

        File[] files = folder.listFiles(file -> StringUtils.endsWithAny(file.getName(), ".txt"));
        if (files == null || files.length == 0) {
            throw new BusinessException(String.format("%s下面没有任何库存文件", folder.getAbsolutePath()));
        }

        List<String> list = new ArrayList<>(files.length);
        for (File file : files) {
            list.add(file.getAbsolutePath());
        }
        return list;
    }

    /**
     * 从库存报表文件中读取ASIN对应的SKU
     *
     * @param asins ASIN集合
     * @param invFilePaths 库存报表文件完整路径
     */
    public Map<String, Set<String>> readSKU(Collection<String> asins, String... invFilePaths) {
        if (CollectionUtils.isEmpty(asins)) {
            return null;
        }

        long start = System.currentTimeMillis();
        Map<String, Boolean> map = new HashMap<>();
        for (String asin : asins) {
            map.put(asin, true);
        }

        Map<String, Set<String>> result = new HashMap<>();
        for (String invFilePath : invFilePaths) {
            File file = new File(invFilePath);
            if (!Strings.containsAnyIgnoreCase(file.getName(), ".txt")) {
                throw new BusinessException(String.format("Only txt inventory report file is acceptable, while %s is not", invFilePath));
            }
            BufferedInputStream fis = null;
            BufferedReader reader = null;

            try {
                fis = new BufferedInputStream(new FileInputStream(file));
                reader = new BufferedReader(new InputStreamReader(fis, Constants.UTF8), BUFFER_SIZE);
                String line;
                int i = 0, skuCount = 0;
                while ((line = reader.readLine()) != null) {
                    i++;
                    if (i == 1 && !Strings.containsAnyIgnoreCase(line, HEADERS)) {
                        throw new BusinessException(String.format("%s is not a valid inventory report file. The first line is '%s', not as expected header '%s'",
                                invFilePath, line, StringUtils.join(HEADERS, Constants.TAB)));
                    }

                    String[] array = StringUtils.split(line, Constants.TAB);
                    if (array.length < 4) {
                        continue;
                    }
                    String sku = array[0], asin = array[1];
                    if (map.get(asin) == null) {
                        continue;
                    }

                    Set<String> skus = result.get(asin);
                    if (skus == null) {
                        skus = new HashSet<>();
                        skus.add(sku);
                        result.put(asin, skus);
                    } else {
                        skus.add(sku);
                    }
                    skuCount++;
                }
                LOGGER.info("从库存报表文件{}中寻找{}个ASIN对应的SKU记录完成。实际结果{}条，SKU{}条，耗时{}",
                        file.getName(), asins.size(), result.size(), skuCount, Strings.formatElapsedTime(start));
            } catch (IOException e) {
                throw new BusinessException(e);
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(fis);
            }
        }

        // 移除空白记录
        result.entrySet().removeIf(item -> CollectionUtils.isEmpty(item.getValue()));
        return result;
    }

    /**
     * 从给定的库存报表文件中读取给定ASIN集合对应的SKU
     */
    public static List<String> getSKUs(List<String> asins, String invFilePath) {
        if (CollectionUtils.isEmpty(asins)) {
            return null;
        }

        long start = System.currentTimeMillis();
        File file = new File(invFilePath);
        if (!Strings.containsAnyIgnoreCase(file.getName(), ".txt")) {
            throw new BusinessException(String.format("Only txt inventory report file is acceptable, while %s is not", invFilePath));
        }
        BufferedInputStream fis = null;
        BufferedReader reader = null;

        Map<String, Boolean> map = new HashMap<>();
        for (String asin : asins) {
            map.put(asin, true);
        }

        List<String> skus = new ArrayList<>();
        try {
            fis = new BufferedInputStream(new FileInputStream(file));
            reader = new BufferedReader(new InputStreamReader(fis, Constants.UTF8), BUFFER_SIZE);
            String line;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    i++;
                    if (i == 1) {
                        continue;
                    }

                    String[] array = StringUtils.split(line, Constants.TAB);
                    if (array.length < 4) {
                        continue;
                    }
                    String sku = array[0], asin = array[1];
                    if (map.get(asin) == null) {
                        continue;
                    }
                    skus.add(sku);
                } catch (Exception e) {
                    //
                }
            }
            LOGGER.info("从库存报表文件中寻找{}个ASIN对应的SKU {}条记录完成。耗时{}", asins.size(), skus.size(), Strings.formatElapsedTime(start));
            return skus;
        } catch (IOException e) {
            throw new BusinessException(e);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(fis);
        }
    }
}
