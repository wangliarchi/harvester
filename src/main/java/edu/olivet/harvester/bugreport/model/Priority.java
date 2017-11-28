package edu.olivet.harvester.bugreport.model;

import edu.olivet.foundations.ui.UIText;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 9:59 AM
 */
public enum Priority {
    /**
     * 非常高：严重影响使用或错误影响极大
     */
    VeryHigh(1),
    /**
     * 高
     */
    High(3),
    /**
     * 中：不影响使用
     */
    Middle(7),
    /**
     * 低：非功能问题，比如文字描述不正确，UI行为可改善等等
     */
    Low(14);

    private final int maxWaitDays;

    Priority(int maxWaitDays) {
        this.maxWaitDays = maxWaitDays;
    }

    public int maxWaitDays() {
        return this.maxWaitDays;
    }

    @Override
    public String toString() {
        return UIText.label("label." + this.name().toLowerCase());
    }

    public String tooltip() {
        return UIText.label("tooltip." + this.name().toLowerCase());
    }

    public static void main(String[] args) {
        for (Priority pri : Priority.values()) {
            System.out.println(String.format("label.%s", pri.name().toLowerCase()));
        }
        for (Priority pri : Priority.values()) {
            System.out.println(String.format("tooltip.%s", pri.name().toLowerCase()));
        }
    }
}
