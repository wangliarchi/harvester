package edu.olivet.harvester.utils.common;

import java.text.DecimalFormat;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/5/17 12:15 PM
 */
public class NumberUtils {
    public static final DecimalFormat SINGLE_FORMAT = new DecimalFormat("0.0");
    public static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");
    public static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.000");
    public static final Float ZERO = 0.0f;

    public static float round(float value, int scale) {
        return (float) (Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale));
    }

    public static String toString(float value, int scale) {
        return Float.toString(round(value,scale));
    }
}
