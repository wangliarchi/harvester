package edu.olivet.harvester.common.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spreadsheet header that has basic information only
 *
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 9/21/2017 1:29 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Spreadsheet {
    @JSONField(name = "spreadId")
    private String id;

    @JSONField(name = "spreadName")
    private String title;

    private List<String> sheetNames;
}
