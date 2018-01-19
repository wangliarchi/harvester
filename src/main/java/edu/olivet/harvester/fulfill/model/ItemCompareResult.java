package edu.olivet.harvester.fulfill.model;

import edu.olivet.harvester.common.model.Order;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * 书名比较结果
 *
 * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Oct 24, 2014 11:48:37 AM
 */
@Data
public class ItemCompareResult implements Comparable<ItemCompareResult> {
    public ItemCompareResult(Order order, String isbnName, boolean preCheckPass, boolean manualCheckPass, String preCheckReport) {
        super();
        this.order = order;
        this.row = order.row;
        this.isbn = order.isbn;
        this.isbnName = isbnName;
        this.itemName = order.item_name;
        this.preCheckPass = preCheckPass;
        this.manualCheckPass = manualCheckPass;
        this.preCheckReport = preCheckReport;
    }

    private Order order;
    private int row;
    private String isbn;
    private String isbnName;
    private String itemName;
    private boolean preCheckPass;
    private String preCheckReport;
    private boolean manualCheckPass;


    public boolean isPreCheckPass() {
        return preCheckPass;
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isManualCheckPass() {
        return manualCheckPass;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((isbn == null) ? 0 : isbn.hashCode());
        result = prime * result + ((isbnName == null) ? 0 : isbnName.hashCode());
        result = prime * result + ((itemName == null) ? 0 : itemName.hashCode());
        result = prime * result + (manualCheckPass ? 1231 : 1237);
        result = prime * result + (preCheckPass ? 1231 : 1237);
        result = prime * result + ((preCheckReport == null) ? 0 : preCheckReport.hashCode());
        result = prime * result + row;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ItemCompareResult other = (ItemCompareResult) obj;
        if (isbn == null) {
            if (other.isbn != null) {
                return false;
            }
        } else if (!isbn.equals(other.isbn)) {
            return false;
        }
        if (isbnName == null) {
            if (other.isbnName != null) {
                return false;
            }
        } else if (!isbnName.equals(other.isbnName)) {
            return false;
        }
        if (itemName == null) {
            if (other.itemName != null) {
                return false;
            }
        } else if (!itemName.equals(other.itemName)) {
            return false;
        }
        if (manualCheckPass != other.manualCheckPass) {
            return false;
        }
        if (preCheckPass != other.preCheckPass) {
            return false;
        }
        if (preCheckReport == null) {
            if (other.preCheckReport != null) {
                return false;
            }
        } else if (!preCheckReport.equals(other.preCheckReport)) {
            return false;
        }
        return row == other.row;
    }

    @Override
    public String toString() {
        return "{row:" + row + ", isbn:" + isbn + ", isbnName:" + isbnName + ", itemName:" + itemName + ", preCheckPass:" + preCheckPass + ", preCheckReport:" + preCheckReport +
                ", manualCheckPass:" + manualCheckPass + "}";
    }

    @Override
    public int compareTo(@NotNull ItemCompareResult o) {
        return this.row - o.row;
    }

}
