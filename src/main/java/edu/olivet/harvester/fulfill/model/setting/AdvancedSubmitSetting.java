package edu.olivet.harvester.fulfill.model.setting;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.model.ConfigEnums;
import edu.olivet.harvester.model.OrderEnums;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/7/17 11:06 AM
 */
public class AdvancedSubmitSetting {
    private ConfigEnums.SubmitRange submitRange = ConfigEnums.SubmitRange.ALL;
    private int countLimit;
    private int singleRowNo;
    private int startRowNo;
    private int endRowNo;
    private String multiRows;

    /**
     * 对做单范围高级设置进行校验并返回检查报告
     */
    public String validate() {
        if (submitRange == ConfigEnums.SubmitRange.MULTIPLE && StringUtils.isBlank(multiRows)) {
            return UIText.message("message.range.error.multiple");
        } else if (submitRange == ConfigEnums.SubmitRange.SCOPE && (startRowNo < 2 || endRowNo < 2)) {
            return UIText.message("message.range.error.scope");
        } else if (submitRange == ConfigEnums.SubmitRange.SINGLE && singleRowNo < 2) {
            return UIText.message("message.range.error.single");
        } else if (submitRange == ConfigEnums.SubmitRange.LimitCount && countLimit == 0) {
            return UIText.message("message.range.error.limit");
        } else {
            return null;
        }
    }

    private OrderEnums.Status statusFilterValue;
    private boolean autoLoop;
    private int loopInterval;

    public ConfigEnums.SubmitRange getSubmitRange() {
        return submitRange;
    }

    public void setSubmitRange(ConfigEnums.SubmitRange scopeType) {
        this.submitRange = scopeType;
    }

    public int getSingleRowNo() {
        return singleRowNo;
    }

    public void setSingleRowNo(int singleRowNo) {
        this.singleRowNo = singleRowNo;
    }

    public int getStartRowNo() {
        return startRowNo;
    }

    public void setStartRowNo(int startRowNo) {
        this.startRowNo = startRowNo;
    }

    public int getEndRowNo() {
        return endRowNo;
    }

    public void setEndRowNo(int endRowNo) {
        this.endRowNo = endRowNo;
    }

    public String getMultiRows() {
        return multiRows;
    }

    public void setMultiRows(String multiRows) {
        this.multiRows = multiRows;
    }

    public boolean isAutoLoop() {
        return autoLoop;
    }

    public void setAutoLoop(boolean autoLoop) {
        this.autoLoop = autoLoop;
    }

    public int getLoopInterval() {
        return loopInterval;
    }

    public void setLoopInterval(int loopInterval) {
        this.loopInterval = loopInterval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (this.submitRange) {
            case ALL:
                sb.append(ConfigEnums.SubmitRange.ALL.format());
                break;
            case SINGLE:
                sb.append(ConfigEnums.SubmitRange.SINGLE.format(this.singleRowNo));
                break;
            case SCOPE:
                if (startRowNo > 0 && endRowNo > 0) {
                    sb.append(ConfigEnums.SubmitRange.SCOPE.format(this.startRowNo, this.endRowNo));
                } else {
                    sb.append(UIText.label("label.scope.notdefine"));
                }
                break;
            case LimitCount:
                sb.append(ConfigEnums.SubmitRange.LimitCount.format(this.countLimit));
                break;
            case MULTIPLE:
                sb.append(ConfigEnums.SubmitRange.MULTIPLE.format(this.multiRows));
                break;
        }

        if (this.statusFilterValue != null) {
            sb.append(Constants.COMMA).append(this.statusFilterValue.value());
        }
        return sb.toString();
    }

    public int getCountLimit() {
        return countLimit;
    }

    public void setCountLimit(int countLimit) {
        this.countLimit = countLimit;
    }

    public OrderEnums.Status getStatusFilterValue() {
        return statusFilterValue;
    }

    public void setStatusFilterValue(OrderEnums.Status statusFilterValue) {
        this.statusFilterValue = statusFilterValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (autoLoop ? 1231 : 1237);
        result = prime * result + countLimit;
        result = prime * result + endRowNo;
        result = prime * result + loopInterval;
        result = prime * result + ((multiRows == null) ? 0 : multiRows.hashCode());
        result = prime * result + singleRowNo;
        result = prime * result + startRowNo;
        result = prime * result + ((statusFilterValue == null) ? 0 : statusFilterValue.hashCode());
        result = prime * result + ((submitRange == null) ? 0 : submitRange.hashCode());
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
        AdvancedSubmitSetting other = (AdvancedSubmitSetting) obj;
        if (autoLoop != other.autoLoop) {
            return false;
        }
        if (countLimit != other.countLimit) {
            return false;
        }
        if (endRowNo != other.endRowNo) {
            return false;
        }
        if (loopInterval != other.loopInterval) {
            return false;
        }
        if (multiRows == null) {
            if (other.multiRows != null) {
                return false;
            }
        } else if (!multiRows.equals(other.multiRows)) {
            return false;
        }
        return singleRowNo == other.singleRowNo && startRowNo == other.startRowNo && statusFilterValue == other.statusFilterValue && submitRange == other.submitRange;
    }
}
