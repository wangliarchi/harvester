package edu.olivet.harvester.letters.model;

import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.letters.model.GrayEnums.GrayLetterType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:58 AM
 */
@Data
@NoArgsConstructor
public class Letter {
    private GrayLetterType type;
    private String subject;
    private String body;
    private Order order;

    public String toMessage() {
        return subject + "\n\n" + body;
    }
}
