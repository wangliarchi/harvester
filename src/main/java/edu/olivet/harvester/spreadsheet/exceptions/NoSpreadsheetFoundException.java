package edu.olivet.harvester.spreadsheet.exceptions;

/**
 * Exception when no google spreadsheet found/returned for given spreadsheet id
 */
class NoSpreadsheetFoundException  extends RuntimeException {

    public NoSpreadsheetFoundException(String errorMsg) {
        super(errorMsg);
    }

    public NoSpreadsheetFoundException(Throwable cause) {
        super(cause);
    }
}