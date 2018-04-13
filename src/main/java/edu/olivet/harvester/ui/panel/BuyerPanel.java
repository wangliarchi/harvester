package edu.olivet.harvester.ui.panel;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.CustomProxyConfig;
import com.teamdev.jxbrowser.chromium.dom.DOMElement;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.exception.AuthenticationFailException;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.model.page.LoginPage;
import edu.olivet.harvester.fulfill.service.PSEventListener;
import edu.olivet.harvester.fulfill.service.PSEventListener.Status;
import edu.olivet.harvester.fulfill.service.RuntimePanelObserver;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.utils.ProgressBarComponent;
import edu.olivet.harvester.utils.JXBrowserHelper;
import edu.olivet.harvester.utils.Settings;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/30/17 9:16 AM
 */
public class BuyerPanel extends WebPanel implements RuntimePanelObserver, ProgressBarComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(BuyerPanel.class);

    @Getter
    protected final Country country;

    @Getter
    protected final Account buyer;

    @Getter
    protected Order order;


    public BuyerPanel(Country country, Account buyer, double zoomLevel, CustomProxyConfig proxyConfig) {
        super(new BorderLayout());
        this.country = country;
        this.buyer = buyer;
        this.zoomLevel = zoomLevel;
        this.browserView = JXBrowserHelper.init(this.profilePathName(), zoomLevel, proxyConfig);

        initComponents();
    }

    public BuyerPanel(Country country, Account buyer, CustomProxyConfig proxyConfig) {
        this(country, buyer, -1, proxyConfig);
    }

    public BuyerPanel(Country country, Account buyer) {
        this(country, buyer, -1, null);
    }

    public BuyerPanel(int id, Country country, Account buyer, double zoomLevel) {
        this(country, buyer, zoomLevel, null);
    }

    public String profilePathName() {
        return this.buyer.key() + Constants.HYPHEN + this.country.name();
    }

    @Getter
    private int taskCount = 0;
    private int total = 0;
    private int successCount = 0;
    private int failedCount = 0;
    private static long start;

    public void setOrder(Order order) {
        this.order = order;
        updateInfo();
    }

    public void initProgressBar(int total) {
        successCount = 0;
        failedCount = 0;
        this.total = total;
        progressBar.setMaximum(total);
        progressBar.setValue(0);
        start = System.currentTimeMillis();
        state = PSEventListener.Status.Running;
        updateProgressBar();
    }

    public void updateSuccess() {
        successCount++;
        updateProgressBar();
    }

    public void updateFailed() {
        failedCount++;
        updateProgressBar();
    }


    private void updateProgressBar() {
        int processedTotal = failedCount + successCount;
        progressBar.setValue(processedTotal);
        progressTextLabel.setText(String.format("%d of %d, %d success, %d failed, took %s",
                processedTotal, total, successCount, failedCount, Strings.formatElapsedTime(start)));

        if (progressBar.getMaximum() == processedTotal) {
            state = Status.NotRunning;
        }
    }

    private String tasksInfo = "";

    public void updateTasksInfo(String info) {
        tasksInfo = "\n" + info;
        updateInfo();
    }

    private void updateInfo() {
        if (order != null) {
            String info = String.format("#%s from %s sheet %s row %d. Tasks in queue: %s %s",
                    order.order_id, order.getType().name().toLowerCase(),
                    order.sheetName, order.row, tasksInfo,
                    stopped() ? " - task STOPPED" : "");
            currentRunningInfoLabel.setText(info);
        }
    }


    public void enablePauseButton() {
        pauseButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    public void disablePauseButton() {
        stopButton.setEnabled(false);
        pauseButton.setEnabled(false);
    }

    private PSEventListener.Status state = PSEventListener.Status.NotRunning;

    private void pause() {
        this.state = PSEventListener.Status.Paused;
        pauseButton.setIcon(UITools.getIcon("resume.png"));
        pauseButton.setText("Resume");
    }

    public void resume() {
        this.state = PSEventListener.Status.Running;
        pauseButton.setIcon(UITools.getIcon("pause.png"));
        pauseButton.setText("Pause");
    }

    public void stop() {
        this.state = PSEventListener.Status.Stopped;
        disablePauseButton();
        currentRunningInfoLabel.setText(currentRunningInfoLabel.getText() + " process STOPPED");
    }

    public void taskStopped() {
        currentRunningInfoLabel.setText(currentRunningInfoLabel.getText() + " Task STOPPED");
    }

    public boolean paused() {
        return state == PSEventListener.Status.Paused;
    }

    public boolean stopped() {
        return state == PSEventListener.Status.Stopped;
    }

    public boolean running() {
        return state == Status.Running;
    }

    public void toHomePage() {
        Browser browser = browserView.getBrowser();
        JXBrowserHelper.loadPage(browser, country.baseUrl());
    }

    private void initComponents() {
        JPanel infoPanel = initInfoPanel();

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(browserView, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        //.addComponent(runtimeSettingsPanel)
                        .addComponent(infoPanel, 200, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)

        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(browserView, 250, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addComponent(infoPanel, 20, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )


        );
    }


    private String spending = "0";
    private String budget = "0";

    public void updateSpending(String spending) {
        this.spending = spending;
        updateBudgetInfo();
    }

    public void updateBudget(String budget) {
        this.budget = budget;
        updateBudgetInfo();
    }

    private void updateBudgetInfo() {
        budgetInfoLabel.setText(String.format("%s / %s", spending, budget));
    }


    public void login() {
        LoginPage loginPage = new LoginPage(this);
        loginPage.execute(null);
        for (int i = 0; i < Constants.MAX_REPEAT_TIMES; i++) {
            if (LoginPage.needLoggedIn(browserView.getBrowser())) {
                loginPage = new LoginPage(this);
                loginPage.execute(null);
                WaitTime.Short.execute();
            } else {
                return;
            }
        }

        throw new AuthenticationFailException("Fail to log in");
    }

    public void postFeedback(String orderId, String feedbackContent) {

        String url = String.format("%s/gp/feedback/leave-consolidated-feedback.html?ie=UTF8&orderID=%s", country.baseUrl(), orderId);
        JXBrowserHelper.loadPage(browserView.getBrowser(), url);
        WaitTime.Shorter.execute();
        if (LoginPage.needLoggedIn(browserView.getBrowser())) {
            login();
            JXBrowserHelper.loadPage(browserView.getBrowser(), url);
            WaitTime.Shorter.execute();
        }

        DOMElement feedbackTextarea = JXBrowserHelper.selectElementByCssSelector(browserView.getBrowser(), "#feedbackText");
        if (feedbackTextarea == null) {
            throw new BusinessException("Invalid order id");
        }

        JXBrowserHelper.selectVisibleElement(browserView.getBrowser(), ".starSprite.clickable.rating5").click();
        try {
            JXBrowserHelper.selectElementByName(browserView.getBrowser(), "fulfillmentAnswer").click();
            WaitTime.Shortest.execute();
        } catch (Exception e) {
            //
        }
        try {
            JXBrowserHelper.selectElementByName(browserView.getBrowser(), "itemAsDescribedAnswer").click();
            WaitTime.Shortest.execute();
        } catch (Exception e) {
            //
        }

        try {
            JXBrowserHelper.fillValueForFormField(browserView.getBrowser(), "#feedbackText", feedbackContent);
            WaitTime.Shortest.execute();
        } catch (Exception e) {
            //
        }

        WaitTime.Shortest.execute();
        JXBrowserHelper.selectVisibleElement(browserView.getBrowser(), ".a-button.a-button-primary.fb_submitbutton").click();

        WaitTime.Shorter.execute();

        DOMElement error = JXBrowserHelper.selectVisibleElement(browserView.getBrowser(), ".a-alert.a-alert-error.clientSideErrorMessages.cfb-message-box");
        if (error != null) {
            throw new BusinessException(JXBrowserHelper.textFromElement(error));
        }
    }


    private JPanel initInfoPanel() {
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        JLabel progressLabel = new JLabel();
        progressLabel.setText("Progress:");
        progressTextLabel = new JLabel();
        progressTextLabel.setText("");

        pauseButton = new JButton();
        pauseButton.setText("Pause");
        pauseButton.setIcon(UITools.getIcon("pause.png"));

        stopButton = new JButton();
        stopButton.setIcon(UITools.getIcon("stop.png"));
        stopButton.setText("Stop");
        disablePauseButton();


        JLabel currentRunningTitleLabel = new JLabel("Current Running:");
        currentRunningInfoLabel = new JLabel("no task running yet.");
        currentRunningTitleLabel.setForeground(Color.blue);
        currentRunningInfoLabel.setForeground(Color.blue);
        progressTextLabel.setForeground(Color.blue);

        Font font = currentRunningTitleLabel.getFont();
        currentRunningTitleLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize() - 1));
        currentRunningInfoLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 1));
        progressTextLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 1));

        JLabel budgetLabel = new JLabel("Budget: ");
        budgetInfoLabel = new JLabel("N/A");
        budgetLabel.setFont(new Font(font.getName(), Font.BOLD, font.getSize() - 1));
        budgetInfoLabel.setFont(new Font(font.getName(), Font.PLAIN, font.getSize() - 1));

        pauseButton.addActionListener(evt -> {
            if (paused()) {
                resume();
            } else {
                pause();
            }
        });

        stopButton.addActionListener(evt -> stop());

        JPanel infoPanel = new JPanel();
        GroupLayout layout = new GroupLayout(infoPanel);
        infoPanel.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(progressLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, 60)
                                                        .addComponent(progressBar, 200, 200, 200)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(progressTextLabel)
                                                        .addContainerGap()
                                                )

                                        )
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                                GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addContainerGap()
                                                        .addComponent(stopButton)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(pauseButton)
                                                        .addContainerGap()
                                                )
                                        )
                                        .addContainerGap())
                        .addGroup(
                                layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(currentRunningTitleLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(currentRunningInfoLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(budgetLabel)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(budgetInfoLabel)
                                        .addContainerGap())
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(progressLabel, 30, 30, 30)
                                        .addComponent(progressBar, 15, 15, 15)
                                        .addComponent(progressTextLabel, 30, 30, 30)
                                        .addComponent(pauseButton)
                                        .addComponent(stopButton))
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(currentRunningTitleLabel)
                                        .addComponent(currentRunningInfoLabel)
                                        .addComponent(budgetLabel)
                                        .addComponent(budgetInfoLabel)
                                )
                                .addContainerGap()

                        )
        );

        return infoPanel;
    }

    @Getter
    private JProgressBar progressBar;
    @Getter
    private JLabel progressTextLabel;
    private JButton pauseButton;
    private JButton stopButton;
    private JLabel currentRunningInfoLabel;
    private JLabel budgetInfoLabel;

    public String getKey() {
        return this.country.name() + Constants.HYPHEN + this.buyer.getEmail();
    }

    @Override
    public String getIcon() {
        return getCountry().name().toLowerCase() + "Flag.png";
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setMinimumSize(new Dimension(800, 600));
        frame.setTitle("Seller Panel Demo");
        frame.setVisible(true);
        Settings settings = Settings.load();
        BuyerPanel buyerPanel = new BuyerPanel(1, Country.US, settings.getConfigByCountry(Country.US).getBuyer(), -1.3);
        frame.getContentPane().add(buyerPanel);

        UITools.setDialogAttr(frame, true);
    }
}