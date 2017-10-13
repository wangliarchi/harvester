package edu.olivet.harvester.utils;

import org.apache.commons.lang3.RandomUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/12/17 9:07 PM
 */
public class CheckTime {

    public static CheckTime getInstance() {
        return INSTANCE;
    }

    private static CheckTime INSTANCE = new CheckTime();

    public static final int FIRST_CHECK_MIN_HOUR = 4;
    public static final int FIRST_CHECK_MAX_HOUR = 8;

    public static final int SECOND_CHECK_MIN_HOUR = 15;
    public static final int SECOND_CHECK_MAX_HOUR = 19;

    /**
     * <pre>
     * 随机生成一个体检时间：
     * 上午{@value #FIRST_CHECK_MIN_HOUR}时到{@value #FIRST_CHECK_MAX_HOUR}时
     * 下午{@value #SECOND_CHECK_MIN_HOUR}时到{@value #SECOND_CHECK_MAX_HOUR}时
     * </pre>
     */
    private CheckTime() {
        this.hour1 = RandomUtils.nextInt(FIRST_CHECK_MIN_HOUR, FIRST_CHECK_MAX_HOUR);
        this.hour2 = RandomUtils.nextInt(SECOND_CHECK_MIN_HOUR, SECOND_CHECK_MAX_HOUR);
        this.minute = RandomUtils.nextInt(0, 60);
    }

    private final int hour1;
    private final int hour2;
    private final int minute;

    @Override
    public String toString() {
        return String.format("%02d:%02d,%02d:%02d", hour1, minute, hour2, minute);
    }

    public String cron() {
        return String.format("0 %d %d,%d ? * *", minute, hour1, hour2);
    }

    public static void main(String[] args) {
        CheckTime checkTime = new CheckTime();
        System.out.println(checkTime.toString());
        System.out.println(checkTime.cron());
    }

}
