package edu.olivet.harvester.fulfill;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Order fulfillment range
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 09/19/2017 09:00:00
 */
@Data
@AllArgsConstructor
class OrderRange {

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
