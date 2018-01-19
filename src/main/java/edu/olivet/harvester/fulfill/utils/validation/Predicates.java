package edu.olivet.harvester.fulfill.utils.validation;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.common.model.OrderEnums;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 11:50 AM
 */
public class Predicates {
    /**
     * 过滤无效订单号: 按照亚马逊订单号规则进行检查
     *
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Mar 15, 2015
     */
    public static class OrderIdPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            String orderId;
            if (object instanceof Order) {
                orderId = ((Order) object).order_id;
            } else {
                orderId = object.toString();
            }
            return RegexUtils.Regex.AMAZON_ORDER_NUMBER.isMatched(StringUtils.defaultString(orderId).trim());
        }
    }

    /**
     * 亚马逊账号有效过滤器
     *
     * @author <a href="mailto:nathanael4ever@gmail.com">Nathanael Yang</a> Apr 22, 2015
     */
    public static class ValidAmazonAccountPredicate implements Predicate {
        public static final ValidAmazonAccountPredicate INSTANCE = new ValidAmazonAccountPredicate();

        @Override
        public boolean evaluate(Object object) {
            Account account = (Account) object;
            return account != null && account.valid();
        }
    }


    public static OrderNoPredicate AMAZON_ORDER_NO = new OrderNoPredicate();


    public static class OrderNoPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            String orderId;
            if (object instanceof Order) {
                orderId = ((Order) object).order_number;
            } else {
                orderId = object.toString();
            }
            return RegexUtils.Regex.AMAZON_ORDER_NUMBER.isMatched(StringUtils.defaultString(orderId).trim());
        }
    }

    public static class NotBlankPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            return StringUtils.isNotBlank((String) object);
        }
    }

    public static NotBlankPredicate NOT_BLANK = new NotBlankPredicate();


    public static class CategoryPredicate implements Predicate {
        public CategoryPredicate(OrderEnums.Status status) {
            this.status = status;
        }

        private final OrderEnums.Status status;

        @Override
        public boolean evaluate(Object object) {
            Order o = (Order) object;
            return status.value().equalsIgnoreCase(o.status);
        }
    }


    public static class MultiRowPredicate implements Predicate {
        private Map<Integer, Boolean> rowMap = new HashMap<>();

        public MultiRowPredicate(String multiRows) {
            String[] rows = StringUtils.split(multiRows, Constants.COMMA);
            for (String row : rows) {
                rowMap.put(Integer.parseInt(row), true);
            }
        }

        @Override
        public boolean evaluate(Object object) {
            Order o = (Order) object;
            return rowMap.get(o.row) != null;
        }
    }


    public static class ScopePredicate implements Predicate {
        private final int startRowNo;
        private final int endRowNo;

        public ScopePredicate(int startRowNo, int endRowNo) {
            this.startRowNo = startRowNo;
            this.endRowNo = endRowNo;
        }

        @Override
        public boolean evaluate(Object object) {
            Order o = (Order) object;
            return o.row >= startRowNo && o.row <= endRowNo;
        }
    }


    public static class SingleRowPredicate implements Predicate {
        public SingleRowPredicate(int rowNo) {
            this.rowNo = rowNo;
        }

        private int rowNo;

        @Override
        public boolean evaluate(Object object) {
            Order o = (Order) object;
            return o.row == rowNo;
        }
    }

    public static class MultiOrderLetterPredicate implements Predicate {
        //private static final Logger logger = LoggerFactory.getLogger(MultiOrderLetterPredicate.class);

        public MultiOrderLetterPredicate(List<Order> orders) {
            super();
            this.orders = orders;
        }

        class OrderIdStatistic {
            public OrderIdStatistic(String orderId, int total, int finished) {
                super();
                this.orderId = orderId;
                this.total = total;
                this.finished = finished;
            }

            public String orderId;
            public int total;
            public int finished;

            @Override
            public String toString() {
                return "[" + orderId + ", " + total + ", " + finished + "]";
            }
        }

        private final List<Order> orders;
        private Map<String, OrderIdStatistic> statisticCache;

        public Map<String, OrderIdStatistic> getCardinalityMap() {
            Map<String, OrderIdStatistic> map = new HashMap<>();
            Map<String, Integer> countMap = new HashMap<>();
            Map<String, Integer> finishedMap = new HashMap<>();

            for (Order order : orders) {


                countMap.merge(order.order_id, 1, (a, b) -> a + b);

            }

            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                String orderId = entry.getKey();
                int total = entry.getValue();
                int finish = finishedMap.get(orderId) == null ? 1 : finishedMap.get(orderId);
                if (total < 2 || total > finish) {
                    continue;
                }

                OrderIdStatistic stat = new OrderIdStatistic(orderId, total, finish);
                map.put(orderId, stat);
            }

            return map;
        }

        @Override
        public boolean evaluate(Object object) {
            if (statisticCache == null) {
                statisticCache = getCardinalityMap();
            }
            return false;
        }
    }
}
