package edu.olivet.harvester.fulfill.service.shipping;


import edu.olivet.harvester.fulfill.model.ShippingEnums.ShippingSpeed;
import edu.olivet.harvester.fulfill.model.ShippingOption;
import edu.olivet.harvester.model.Order;

import java.util.List;

/**
 * 运输方式决定接口，不同国家对应不同实现
 */
public interface ShippingHandler {


    List<ShippingOption> getValidateOptions(Order order, List<ShippingOption> shippingOptions);

    ShippingOption determineShipOption(Order order, List<ShippingOption> shippingOptions);

    /**
     * 获取当前订单最终选择的运输方式
     *
     * @param order 当前订单
     * @return 运输方式 {@link ShippingSpeed}
     */
    ShippingSpeed determineFinalSpeed(Order order);


}
