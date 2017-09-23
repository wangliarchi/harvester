package edu.olivet.harvester.model;

import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.foundations.utils.Strings;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 美国各州枚举定义
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
public enum State {
    AL(470, 310, "Alabama", 0),
    AK(80, 370, "Alaska", 0),
    AZ(140, 270, "Arizona", 0.0831),
    AR(390, 280, "Arkansas", 0),
    CA(50, 200, "California", 0.0875),
    CO(225, 210, "Colorado", 0),
    CT(620, 140, "Connecticut", 0.082),
    DE(600, 190, "Delaware", 0),
    FL(550, 380, "Florida", 0),
    GA(510, 300, "Georgia", 0.07),
    HI(200, 410, "Hawaii", 0),
    ID(140, 120, "Idaho", 0),
    IL(425, 195, "Illinois", 0),
    IN(465, 195, "Indiana", 0),
    IA(375, 170, "Iowa", 0),
    KS(310, 225, "Kansas", 0.0716),
    KY(480, 235, "Kentucky", 0.0602),
    LA(400, 350, "Louisiana", 0),
    ME(640, 75, "Maine", 0),
    MD(580, 185, "Maryland", 0),
    MA(620, 125, "Massachusetts", 0.063),
    MI(475, 140, "Michigan", 0),
    MN(360, 100, "Minnesota", 0),
    MS(430, 310, "Mississippi", 0),
    MO(390, 225, "Missouri", 0),
    MT(200, 80, "Montana", 0),
    NE(300, 170, "Nebraska", 0),
    NV(95, 180, "Nevada", 0),
    NH(625, 100, "New Hampshire", 0),
    NJ(600, 175, "New Jersey", 0.0702),
    NM(215, 280, "New Mexico", 0),
    NY(580, 130, "New York", 0.0888),
    NC(560, 250, "North Carolina", 0),
    ND(300, 80, "North Dakota", 0.0602),
    OH(500, 185, "Ohio", 0),
    OK(330, 275, "Oklahoma", 0),
    OR(65, 100, "Oregon", 0),
    PA(560, 170, "Pennsylvania", 0.0602),
    RI(630, 135, "Rhode Island", 0),
    SC(540, 280, "South Carolina", 0),
    SD(295, 130, "South Dakota", 0),
    TN(460, 260, "Tennessee", 0),
    TX(300, 335, "Texas", 0.0831),
    UT(150, 195, "Utah", 0),
    VT(610, 95, "Vermont", 0),
    VA(555, 225, "Virginia", 0.0602),
    WA(85, 50, "Washington", 0.0946),
    WV(540, 200, "West Virginia", 0.0602),
    WI(410, 120, "Wisconsin", 0.0544),
    WY(210, 145, "Wyoming", 0),
    //-----本土之外岛屿，运输可能需国际快递-----//
    AS(0, 0, "American Samoa", 0),
    GU(0, 0, "Guam", 0),
    MP(0, 0, "Northern Mariana Islands", 0),
    PR(0, 0, "Puerto Rico", 0),
    VI(0, 0, "U.S. Virgin Islands", 0);

    /**
     * 地址位置横坐标
     */
    private final int x;
    /**
     * 地址位置纵坐标
     */
    private final int y;
    /**
     * 完整名称描述
     */
    private final String desc;
    /**
     * 税率
     */
    final double taxRate;

    public int axisX() {
        return x;
    }

    public int axisY() {
        return y;
    }

    public String desc() {
        return desc;
    }

    private static final Map<String, State> cache = new HashMap<>();

    public static State parse(String src) {
        String lowerSrc = src.toLowerCase();

        if (cache.isEmpty()) {
            for (State state : State.values()) {
                cache.put(state.desc.toLowerCase(), state);
            }
        }

        if (cache.get(lowerSrc) != null) {
            return cache.get(lowerSrc);
        }

        for (State state : State.values()) {
            if (state.desc.equalsIgnoreCase(lowerSrc) || state.name().equalsIgnoreCase(lowerSrc) ||
                Strings.containsAnyIgnoreCase(lowerSrc, state.desc)) {
                cache.put(lowerSrc, state);
                return state;
            }

            String desc = state.desc.replaceAll(Regex.NON_ALPHA_LETTERS.val(), StringUtils.EMPTY);
            lowerSrc = src.replaceAll(Regex.NON_ALPHA_LETTERS.val(), StringUtils.EMPTY);
            if (desc.equalsIgnoreCase(lowerSrc) || Strings.containsAnyIgnoreCase(lowerSrc, desc)) {
                cache.put(src.toLowerCase(), state);
                return state;
            }
        }

        throw new IllegalArgumentException(String.format("Illegal State Name: %s", src));
    }

    State(int x, int y, String desc, double taxRate) {
        this.x = x;
        this.y = y;
        this.desc = desc;
        this.taxRate = taxRate;
    }
}
