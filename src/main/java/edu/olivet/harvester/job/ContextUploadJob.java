package edu.olivet.harvester.job;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.release.Version;
import edu.olivet.foundations.release.VersionManager;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.TeamViewerFetcher;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.utils.AnyDeskFetcher;
import edu.olivet.harvester.utils.Settings;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jsoup.Jsoup;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/12/17 8:42 PM
 */
public class ContextUploadJob extends AbstractBackgroundJob {
    @Data
    private static class InstallationContext {
        private String sid;
        private String version;
        private String os;
        private String jvm;
        private String teamviewerId;
        private String anyDeskId = "";
        private String checkTime;
        private String marketPlaces;
        private String timeZone;
        private String freePhysicalMemorySize;
        private String totalPhysicalMemorySize;
        private String systemCpuLoad;
    }

    private static final String APPS_URL = "https://script.google.com/macros/s/AKfycbxdEFwL8oO7ahkB0ICe7Wf0TuMaYG01ntQrm3zXWWFVfVJNtcgo/exec";

    @Override
    public void execute() {
        try {
            long start = System.currentTimeMillis();
            InstallationContext context = this.buildContext();
            logger.info("Harvester installation context: {}.", JSON.toJSONString(context));

            String result = Jsoup.connect(APPS_URL + "?context=" +
                    Strings.encode(JSON.toJSONString(context))).timeout(12000).execute().body();
            logger.info("Upload installation context of {} finished in {}, result: {}", context.getSid(),
                    Strings.formatElapsedTime(start), result);
        } catch (Exception e) {
            logger.error("Failed to upload installation context of: ", e);
        }
    }

    private InstallationContext buildContext() {
        InstallationContext context = new InstallationContext();
        Settings settings = Settings.load();

        context.setSid(settings.getSid() + "(" + SystemUtils.USER_NAME + ")");

        VersionManager versionManager = ApplicationContext.getBean(VersionManager.class);
        Version version = versionManager.getCurrentVersion(Harvester.APP_NAME);
        context.setVersion(version.getCode().toString());
        context.setOs(SystemUtils.OS_NAME);
        context.setJvm(String.format("%s:%s", SystemUtils.JAVA_VERSION, SystemUtils.JAVA_VM_NAME));
        context.setTeamviewerId(new TeamViewerFetcher().execute());
        context.setAnyDeskId(new AnyDeskFetcher().execute());
        context.setCheckTime("");

        StringBuilder sb = new StringBuilder();
        settings.getConfigs().forEach(config -> sb.append(Constants.COMMA_WHITESPACE).append(config.getCountry().name()));
        context.setMarketPlaces(sb.toString().replaceFirst(Constants.COMMA, StringUtils.EMPTY).trim());

        context.setTimeZone(TimeZone.getDefault().getDisplayName(Locale.US));

        getMemoryAndCPUInfo(context);
        return context;
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "" + ("KMGTPE").charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }


    public void getMemoryAndCPUInfo(InstallationContext context) {
        //FreePhysicalMemorySize
        //TotalPhysicalMemorySize
        //SystemCpuLoad
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.getName().startsWith("get") && Modifier.isPublic(method.getModifiers())) {
                Object value;
                try {
                    value = method.invoke(operatingSystemMXBean);
                } catch (Exception e) {
                    value = e;
                }

                if ("getFreePhysicalMemorySize".equalsIgnoreCase(method.getName())) {
                    try {
                        float v = Float.parseFloat(value.toString());
                        context.setFreePhysicalMemorySize(humanReadableByteCount((long) v));
                    } catch (Exception e) {
                        //
                    }
                }

                if ("getTotalPhysicalMemorySize".equalsIgnoreCase(method.getName())) {
                    try {
                        float v = Float.parseFloat(value.toString());
                        context.setTotalPhysicalMemorySize(humanReadableByteCount((long) v));
                    } catch (Exception e) {
                        //
                    }
                }

                if ("getProcessCpuLoad".equalsIgnoreCase(method.getName())) {
                    try {
                        float v = Float.parseFloat(value.toString());
                        context.setSystemCpuLoad(Float.toString(v * 100) + "%");
                    } catch (Exception e) {
                        //
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        ContextUploadJob bean = ApplicationContext.getBean(ContextUploadJob.class);
        InstallationContext ctx = bean.buildContext();
        System.out.println(Strings.encode(JSON.toJSONString(JSON.toJSONString(ctx, true))));
        System.exit(0);
    }
}
