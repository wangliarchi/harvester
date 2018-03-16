package edu.olivet.harvester.job;

import edu.olivet.foundations.db.DBManager;
import edu.olivet.foundations.job.AbstractBackgroundJob;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.harvester.common.model.CronjobLog;
import edu.olivet.harvester.finance.InvoiceDownloader;
import edu.olivet.harvester.finance.model.InvoiceTask;
import org.apache.commons.collections4.CollectionUtils;
import org.nutz.dao.Cnd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 3:00 PM
 */
public class DownloadInvoiceTaskJob extends AbstractBackgroundJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadInvoiceTaskJob.class);


    @Override
    public void execute() {
        DBManager dbManager = ApplicationContext.getBean(DBManager.class);

        List<InvoiceTask> list = dbManager.query(InvoiceTask.class, Cnd.where("status", "!=", "Done"));
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        ApplicationContext.getBean(InvoiceDownloader.class).download(list);

        CronjobLog log = new CronjobLog();
        log.setId(this.getClass().getName() + Dates.nowAsFileName());
        log.setJobName(this.getClass().getName());
        log.setRunTime(new Date());


        dbManager.insert(log, CronjobLog.class);
    }

    @Override
    public void runIfMissed(Date nextTriggerTime) {
        //
    }


}
