package edu.olivet.harvester.spreadsheet.exceptions;


/**
 * Exception when no worksheet for given sheet name
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/20/2017 7:52 AM
 */
public class NoWorksheetFoundException extends RuntimeException {

    public NoWorksheetFoundException(String errorMsg) {
        super(errorMsg);
    }

    public NoWorksheetFoundException(Throwable cause) {
        super(cause);
    }
}