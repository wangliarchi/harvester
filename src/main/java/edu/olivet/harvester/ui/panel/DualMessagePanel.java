package edu.olivet.harvester.ui.panel;


import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;

public interface DualMessagePanel extends MessagePanel {
    /**
     * 显示一条短消息文本
     *
     * @param msg 短消息文本
     */
    void displayShortMsg(String msg, InformationLevel... infoLevels);

    /**
     * 增加短消息文本区域分割换行符
     */
    void addSeparator();
}
