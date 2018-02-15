package edu.olivet.harvester.fulfill.model;

import edu.olivet.harvester.common.model.Order;
import lombok.Data;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/27/2018 2:42 PM
 */
@Data
public class SubmitResult {
    /**
     * 返回结果代码
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a>
     */
    public enum ReturnCode {
        SUCCESS,
        FAILURE
    }

    public SubmitResult(String result, ReturnCode code) {
        this.result = result;
        this.code = code;
    }

    public SubmitResult(Order order, String result, ReturnCode code) {
        this(result, code);
        this.order = order;
    }

    private Order order;
    private String result;
    private ReturnCode code;


}
