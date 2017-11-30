package edu.olivet.harvester.fulfill.exception;


/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/29/2017 5:55 AM
 */
public class OrderSubmissionException extends RuntimeException {
    public OrderSubmissionException(String errorMsg) {
        super(errorMsg);
    }

    public OrderSubmissionException(Throwable cause) {
        super(cause);
    }
}
