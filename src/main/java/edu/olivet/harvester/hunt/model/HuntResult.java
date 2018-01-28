package edu.olivet.harvester.hunt.model;

import edu.olivet.harvester.common.model.Order;
import lombok.Data;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/27/2018 2:42 PM
 */
@Data
public class HuntResult {
    /**
     * 返回结果代码
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a>
     */
    public static enum ReturnCode {
        SUCCESS,
        FAILURE
    }

    public HuntResult(String result, ReturnCode code) {
        this.result = result;
        this.code = code;
    }

    public HuntResult(Order order, String result, ReturnCode code) {
        this(result, code);
        this.order = order;
    }

    private Order order;
    private String result;
    private ReturnCode code;


}
