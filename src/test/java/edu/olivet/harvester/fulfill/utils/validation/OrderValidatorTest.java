package edu.olivet.harvester.fulfill.utils.validation;

import com.google.inject.Inject;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.common.model.Order;
import edu.olivet.harvester.fulfill.utils.validation.OrderValidator.Validator;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class OrderValidatorTest extends BaseTest {
    @Inject OrderValidator orderValidator;

    @Test
    public void notSelfOrder() {
        Order order = prepareOrder();
        order.remark = "change address";

        String result = orderValidator.validWithValidators(order, Validator.AddressNotChanged);
        System.out.println(result);

        assertTrue(StringUtils.isNotBlank(result));

    }

}