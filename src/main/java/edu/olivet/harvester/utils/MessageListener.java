package edu.olivet.harvester.utils;


import com.google.inject.Singleton;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.MessagePanel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.VirtualMessagePanel;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.Dates;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.foundations.utils.WaitTime;
import edu.olivet.harvester.fulfill.utils.OrderCountryUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.ui.panel.DualMessagePanel;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 消息队列，接受、显示消息，以便业务类不与界面类耦合起来
 *
 * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2015年9月11日 下午5:00:12
 */
@Singleton
public class MessageListener implements MessageQueue, MessagePanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);


    /**
     * 消息文本显示位置，目前实际只用到左侧面板(显示较短的消息文本)和右侧面板(显示较长的消息文本)
     *
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2015年9月11日 下午6:08:38
     */
    public enum Position {
        Left, Right
    }

    /**
     * 一条消息，各种业务处理类会生产不同消息在主程序界面上显示
     *
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> 2015年9月11日 下午6:10:08
     */
    static class Message {
        Message(String content, Position position, InformationLevel... infoLevels) {
            this(content, position, false, infoLevels);
        }

        Message(String content, Position position, boolean wrapLine, InformationLevel... infoLevels) {
            this.content = content;
            this.position = position;
            this.wrapLine = wrapLine;
            this.infoLevels = infoLevels;
        }

        final String content;
        final Position position;
        final boolean wrapLine;
        final InformationLevel[] infoLevels;

        @Override
        public String toString() {
            return this.position + Constants.COMMA + StringUtils.abbreviate(content, 40);
        }
    }

    /**
     * 初始消息队列总数
     */
    private static final int INIT_MSG_SIZE = 200;
    /**
     * 消息面板，依赖接口，其实现可以是Harvester或其他任意实现此接口的Swing UI组件
     */
    private MessagePanel panel = new VirtualMessagePanel();
    private final BlockingQueue<Message> messages = new ArrayBlockingQueue<>(INIT_MSG_SIZE);

    /**
     * 获取当前消息队列的概况
     */
    private String status() {
        return String.format("Messages: %s(%s)", messages.size(), messages);
    }

    /**
     * 清空消息队列
     */
    public void empty() {
        panel.clearPrevious();
        messages.clear();
    }

    /**
     * 将一条消息加入队列, 因为put在消息池已满的情况下会阻塞等待, 因而凡使用此类的地方都最好实例化之后调用start方法进行消费, 避免无限等待
     */
    private void add(Message msg) {
        if (panel instanceof VirtualMessagePanel) {
            return;
        }
        try {
            messages.add(msg);
        } catch (IllegalStateException e) {
            messages.clear();
        } catch (Exception e) {
            LOGGER.warn("加入消息队列过程中出现其他异常:{}", e.getMessage());
            messages.clear();
        }
    }

    @Override
    public void addShortMsg(String msg, InformationLevel... infoLevels) {
        this.add(new Message(msg, Position.Left, infoLevels));
    }

    @Override
    public void wrapLineShortMsg(String msg, InformationLevel... infoLevels) {
        this.add(new Message(msg, Position.Left, true, infoLevels));
    }

    @Override
    public void addLongMsg(String msg, InformationLevel... infoLevels) {
        this.add(new Message(msg, Position.Right, infoLevels));
    }


    public void addMsg(String msg, InformationLevel... infoLevels) {
        this.addLongMsg(msg, infoLevels);
    }

    public void addMsg(List<String> msgs, InformationLevel... infoLevels) {
        msgs.forEach(msg -> this.addLongMsg(msg, infoLevels));

    }

    public void addMsg(Order order, String msg, InformationLevel... infoLevels) {
        Country country = OrderCountryUtils.getMarketplaceCountry(order);
        this.addLongMsg(String.format("%s %s - %s - row %d - %s - %s",
                country != null ? (country.europe() ? "EU" : country.name()) : order.getSpreadsheetId(), order.type().name(),
                order.sheetName, order.row, order.order_id, msg), infoLevels);
    }

    @Override
    public void wrapLineLongMsg(String msg, InformationLevel... infoLevels) {
        this.add(new Message(msg, Position.Right, true, infoLevels));
    }

    /**
     * 启动监听线程，监听、消费各业务类生产的消息
     */
    public void start() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    Message msg = messages.take();
                    if (msg.position == Position.Left) {
                        ((DualMessagePanel) panel).displayShortMsg(msg.content, msg.infoLevels);
                        if (msg.wrapLine) {
                            ((DualMessagePanel) panel).addSeparator();
                        }
                    } else {
                        panel.displayMsg(msg.content, msg.infoLevels);
                        if (msg.wrapLine) {
                            panel.addMsgSeparator();
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("从消息队列中获取记录时出现异常:", e);
                }
            }
        }, "MessageListener").start();
    }

    public void setContainer(MessagePanel panel) {
        this.panel = panel;
    }

    public MessagePanel getContainer() {
        return this.panel;
    }

    public static void main(String[] args) {

        final MessageListener listener = new MessageListener();
        listener.setContainer(new ProgressDetail("Message Listener Test"));
        listener.start();

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                int index = 0;
                while (index++ < 20) {
                    Tools.sleep(RandomUtils.nextLong(0, 1000));
                    String content = Thread.currentThread().getName() + "测试消息" + index + ", 时间:" + Dates.now();
                    listener.addLongMsg(content);
                    System.out.println(listener.status());
                }
            }, "Producer" + i);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        WaitTime.Long.execute();
        System.exit(0);
    }

    @Override
    public void clearPrevious() {

    }

    @Override
    public void displayMsg(String msg, InformationLevel... infoLevels) {
        panel.displayMsg(msg, infoLevels);
    }

    @Override
    public void displayMsg(String msg, Logger logger, InformationLevel... infoLevels) {
        panel.displayMsg(msg, logger, infoLevels);
    }

    @Override
    public void addMsgSeparator() {
        panel.addMsgSeparator();
    }

    @Override
    public void wrapLineMsg(String msg, InformationLevel... infoLevels) {
        panel.wrapLineMsg(msg, infoLevels);
    }

    @Override
    public void wrapLineMsg(String msg, Logger logger, InformationLevel... infoLevels) {
        panel.wrapLineMsg(msg, logger, infoLevels);
    }
}
