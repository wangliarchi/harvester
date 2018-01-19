package edu.olivet.harvester.utils;


import edu.olivet.foundations.ui.InformationLevel;

/**
 * 消息队列处理接口
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 1/21/2016 7:53 PM
 */
interface MessageQueue {
    /**
     * 添加、显示一条短文本消息
     */
    void addShortMsg(String msg, InformationLevel... infoLevels);

    /**
     * 添加、换行显示一条短文本消息
     */
    void wrapLineShortMsg(String msg, InformationLevel... infoLevels);

    /**
     * 添加、显示一条长文本消息
     */
    void addLongMsg(String msg, InformationLevel... infoLevels);

    /**
     * 添加、换行显示一条长文本消息
     */
    void wrapLineLongMsg(String msg, InformationLevel... infoLevels);
}
