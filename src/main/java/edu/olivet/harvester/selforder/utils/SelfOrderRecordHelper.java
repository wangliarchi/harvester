package edu.olivet.harvester.selforder.utils;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.harvester.selforder.model.SelfOrder;
import edu.olivet.harvester.selforder.model.SelfOrderRecord;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/2/2018 6:08 AM
 */
public class SelfOrderRecordHelper {


    public void setColumnValue(int col, String value, SelfOrderRecord selfOrder) {
        Column orderColumn = Column.get(col);
        setColumnValue(orderColumn, value, selfOrder);
    }


    private static final Map<String, Field> RECORD_FIELDS_CACHE = new HashMap<>();

    public void setColumnValue(Column orderColumn, String value, SelfOrderRecord selfOrder) {
        if (orderColumn != null) {
            String fieldName = orderColumn.name();
            Field filed = RECORD_FIELDS_CACHE.computeIfAbsent(fieldName, s -> {
                try {
                    return SelfOrderRecord.class.getDeclaredField(s);
                } catch (NoSuchFieldException e) {
                    return null;
                }
            });
            if (filed == null) {
                return;
            }

            try {
                filed.set(selfOrder, StringUtils.defaultString(value).trim());
            } catch (IllegalAccessException e) {
                throw new BusinessException(e);
            }
        }
    }


    public enum Column {
        orderDate(1),
        storeName(2),
        sellerId(3),
        country(4),
        asin(5),
        promoCode(6),
        buyerAccountCode(7),
        buyerAccountEmail(8),
        orderNumber(9),
        cost(10),
        feedbackLeft(11),
        feedback(12),
        feedbackDate(13),
        uniqueCode(14);
        private final int number;

        /**
         * 获取对应的列号
         */
        public int number() {
            return number;
        }

        public int index() {
            return number - 1;
        }

        Column(int number) {
            this.number = number;
        }

        public static @Nullable Column get(int number) {
            for (Column column : Column.values()) {
                if (column.number == number) {
                    return column;
                }
            }
            return null;
        }
    }
}
