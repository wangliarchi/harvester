package edu.olivet.harvester.spreadsheet.exceptions;

public class NoOrdersFoundInWorksheetException   extends RuntimeException {

    public NoOrdersFoundInWorksheetException(String errorMsg) {
        super(errorMsg);
    }

    public NoOrdersFoundInWorksheetException(Throwable cause) {
        super(cause);
    }
}
