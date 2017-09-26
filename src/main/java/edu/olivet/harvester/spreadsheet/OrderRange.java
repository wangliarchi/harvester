package edu.olivet.harvester.spreadsheet;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Order fulfillment range
 * @author <a href="mailto:mengyang0427@gmail.com">Nathanael Yang</a> 9/14/2017 10:50 PM
 */
@Data
@AllArgsConstructor
public class OrderRange {

    private String sheetName;

    private Integer beginRow;

    private Integer endRow;

    boolean contains(int row) {
        if (beginRow == null && endRow == null) {
            return true;
        } else if (beginRow != null && endRow != null) {
            return row >= beginRow && row <= endRow;
        } else if (beginRow != null) {
            return row >= beginRow;
        } else {
            return row <= endRow;
        }
    }

    boolean beyond(int row) {
        return endRow != null && row > endRow;
    }

}
