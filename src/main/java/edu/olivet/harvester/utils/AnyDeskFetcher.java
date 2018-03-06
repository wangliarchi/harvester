package edu.olivet.harvester.utils;

import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * A utility feature to upload AnyDesk snapshot to dropbox server for support convenience
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/12/17 9:08 PM
 */
public class AnyDeskFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnyDeskFetcher.class);
    private static final String ANYDESK_PATH = "C:\\Program Files (x86)\\AnyDesk\\AnyDesk.exe";

    public String execute() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            LOGGER.warn("This trick only works on Windows platform, however your OS is " + SystemUtils.OS_NAME);
            return StringUtils.EMPTY;
        }

        File exe = new File(ANYDESK_PATH);
        // See: http://superuser.com/questions/75614/take-a-screen-shot-from-command-line-in-windows/943947#943947
        if (!exe.exists() || !exe.isFile()) {
            LOGGER.warn("It seems AnyDesk was not installed at this computer, we cannot " +
                    "find expected executable file '{}'.", ANYDESK_PATH);
            return StringUtils.EMPTY;
        }

        // Detect teamviewer version simply by checking log file name

        String confFilePath = String.format("C:\\Users\\%s\\AppData\\Roaming\\AnyDesk\\system.conf", SystemUtils.USER_NAME);

        String id = retrieveId(confFilePath);

        if (StringUtils.isNotBlank(id)) {
            return id;
        }


        return StringUtils.EMPTY;
    }

    /**
     * Retrieve anydesk id from conf file contents
     *
     * @param confFilePath anydesk conf file full path
     * @return teamviewer id or empty if none can be found
     */
    String retrieveId(String confFilePath) {
        File file = new File(confFilePath);
        if (!file.exists() || !file.isFile()) {
            return StringUtils.EMPTY;
        }

        String id = StringUtils.EMPTY;
        BufferedInputStream fis = null;
        BufferedReader reader = null;
        try {
            fis = new BufferedInputStream(new FileInputStream(file));
            reader = new BufferedReader(new InputStreamReader(fis, Constants.UTF8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (RegexUtils.containsRegex(line, "ad.anynet.id=+[0-9]{9}")) {
                    id = RegexUtils.getMatched(line, "[0-9]{9}");
                    LOGGER.debug("Retrieve anydesk id {} from '{}'.", id, line);
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read and analyze anydesk log file {}:", confFilePath, e);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(fis);
        }

        return id;
    }

    public static void main(String[] args) {
        System.out.println(new AnyDeskFetcher().execute());
        System.exit(0);
    }
}
