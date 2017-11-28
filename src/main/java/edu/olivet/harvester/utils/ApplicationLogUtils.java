package edu.olivet.harvester.utils;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Directory;
import edu.olivet.harvester.model.ConfigEnums.Log;
import edu.olivet.harvester.ui.Harvester;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 10:48 AM
 */
public class ApplicationLogUtils {
    public static File compressApplicationLogs(String context, boolean onlyToday) throws ZipException {
        ZipParameters zipParams = new ZipParameters();
        if (!onlyToday) {
            zipParams.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        }
        zipParams.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

        String zipPath = Directory.Tmp.path() + File.separator + context + "_" + Dates.nowAsFileName() + ".zip";
        ZipFile zip = new ZipFile(zipPath);
        if (onlyToday) {
            Log[] logs = Log.values();
            List<Log> logs4Sent = new ArrayList<>(logs.length);
            for (Log log : logs) {
                if (log.valid()) {
                    logs4Sent.add(log);
                }
            }
            if (CollectionUtils.isEmpty(logs4Sent)) {
                throw new BusinessException(UIText.message("error.logs.invalid", Harvester.APP_NAME));
            }

            for (Log log : logs4Sent) {
                File logFile = log.file();
                zip.addFile(logFile, zipParams);
            }
        } else {
            zip.addFolder(Directory.Log.path(), zipParams);
        }

        return new File(zipPath);

    }
}
