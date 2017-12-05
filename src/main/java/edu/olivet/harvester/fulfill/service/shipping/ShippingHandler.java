package edu.olivet.harvester.fulfill.service.shipping;


import edu.olivet.harvester.fulfill.exception.Exceptions;
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


    /**
     * 对订单的运费进行校验，不能超过设定的上限
     *
     * @param shippingFee 下单时的实际每件物品运费
     * @param order       当前订单
     * @throws Exceptions.ShippingFeeTooHighException
     */
    void checkFee(float shippingFee, Order order) throws Exceptions.ShippingFeeTooHighException;
}
