package edu.olivet.harvester.spreadsheet.model;


import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order fulfillment range
 *
 * @author <a href="mailto:mengyang0427@gmail.com">Nathanael Yang</a> 9/14/2017 10:50 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public String toString() {
        return JSON.toJSONString(this);
    }

    public String desc() {
        StringBuilder sb = new StringBuilder(sheetName);
        if (beginRow == null && endRow == null) {
            sb.append(" ALL");
        } else if (beginRow != null && endRow != null) {
            sb.append(" ").append(beginRow).append("-").append(endRow);
        } else if (beginRow != null) {
            sb.append(" ").append(beginRow).append(" - end");
        } else {
            sb.append(" to").append(endRow);
        }

        return sb.toString();

    }
}
