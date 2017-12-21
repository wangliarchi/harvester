package edu.olivet.harvester.export.model;

import edu.olivet.foundations.amazon.Country;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/20/2017 4:25 PM
 */
@Data
@NoArgsConstructor
public class OrderExportParams {
    List<Country> marketplaces;
    Date fromDate;
    Date toDate;
}
