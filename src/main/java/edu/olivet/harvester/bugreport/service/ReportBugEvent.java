package edu.olivet.harvester.bugreport.service;

import com.google.inject.Inject;
import edu.olivet.foundations.aop.Repeat;
import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.release.VersionManager;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.TeamViewerFetcher;
import edu.olivet.foundations.utils.TemplateHelper;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.bugreport.model.Bug;
import edu.olivet.harvester.bugreport.ui.BugReportDialog;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.ui.Harvester;
import edu.olivet.harvester.ui.events.HarvesterUIEvent;
import edu.olivet.harvester.utils.ApplicationLogUtils;
import edu.olivet.harvester.utils.Settings;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 10:24 AM
 */
public class ReportBugEvent implements HarvesterUIEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportBugEvent.class);

    @Inject
    private DBManager dbManager;
    @Inject
    VersionManager versionManager;

    @Inject
    ErrorAlertService errorAlertService;

    @Inject
    private TemplateHelper templateHelper;

    private static final String REPORT_BUG_EMAIL_TEMPLATE = "BugReport";

    @Inject
    TeamViewerFetcher teamViewerFetcher;

    @Override
    @Repeat
    public void execute() {


        Bug bug = new Bug();
        try {
            String teamviewerId = teamViewerFetcher.execute();
            bug.setTeamviewerId(teamviewerId);
        } catch (Exception e) {
            //ignore
        }

        BugReportDialog dialog = UITools.setDialogAttr(new BugReportDialog(bug));

        bug = dialog.getBug();
        if (bug == null) {
            return;
        }
        collectBugContext(bug);

        bug.setId(Tools.generateUUID());
        bug.setReportTime(new Date());

        report(bug);

        dbManager.insert(bug, Bug.class);
        UITools.info(UIText.message("success.bug.report", bug.getId(), bug.getPriority().maxWaitDays()));
    }


    public void report(Bug bug) {
        String subject = StringUtils.defaultIfBlank(bug.getTitle(),
                String.format("Bug Report from %s regarding %s", bug.getContext(), bug.getIssueCategory()));
        subject = String.format("Bug:%s-%s-%s:%s", bug.getContext(), bug.getId(), bug.getPriority(), subject);
        String content = templateHelper.getContent(bug, REPORT_BUG_EMAIL_TEMPLATE);

        File attachment = null;
        try {
            attachment = ApplicationLogUtils.compressApplicationLogs(bug.getContext(), true);
        } catch (ZipException e) {
            //ignore
        }

        errorAlertService.sendMessage(subject, content, bug.getCountry(), attachment);
    }

    void collectBugContext(final Bug bug) {
        Settings settings = Settings.load();
        bug.setContext(settings.getSid() + bug.getCountry().name());
        bug.setVersion(versionManager.getCurrentVersion(Harvester.APP_NAME).abbrev());
        bug.setReporterEmail(settings.getConfigByCountry(bug.getCountry()).getSellerEmail().getEmail());
    }
}
