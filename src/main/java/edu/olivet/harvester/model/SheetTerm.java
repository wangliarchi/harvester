package edu.olivet.harvester.model;

import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.Strings;

/**
 * GoogleAddressValidator Sheet中业务名词常量定义
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
public enum SheetTerm {
    /**
     * Cancel过的订单重新补做
     */
    ReFulfilled(new String[] {"Cancel单补做", "Seller canceled orders", "Seller cancelled orders"}),
    /**
     * New To Good的订单
     */
    NewToGood(new String[] {"n to g客人要的单", "Individual orders"}),
    /**
     * 灰条变白条的订单
     */
    GreyToWhite(new String[] {"Gray To White Orders"}),
    /**
     * 备注，通常用于标记Buyer Cancel等相关订单
     */
    Memo(new String[] {"备注", "Memo"});

    private final String[] keyWords;
    SheetTerm(String[] keyWords) {
        this.keyWords = keyWords;
    }

    public boolean match(String name) {
        return Strings.containsAnyIgnoreCase(name, this.keyWords);
    }

    public static boolean isNewToGood(String sheetName) {
        return NewToGood.match(sheetName) || RegexUtils.containsRegex(sheetName, "n[\\s]*to[\\s]*g");
    }

    /**
     * 在抓取Tracking号和Feedback的场景下，判定该Sheet是否需要略过：Cancel单补做、灰条变白条以及备注等性质的页签均需略过
     */
    public static boolean skip(String sheetName) {
        return SheetTerm.Memo.match(sheetName) || SheetTerm.ReFulfilled.match(sheetName) || SheetTerm.isNewToGood(sheetName);
    }
}
