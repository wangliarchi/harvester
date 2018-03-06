<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
Dear ${order.recipient_name},

This is ${config.userCode}, from ${config.storeName}.

Thank you for your recent purchase of "${order.item_name}" with us. ${order.order_id}

I will be your dedicated customer service support from now on!

If you have any questions regarding this order, just simply reply to this email and let me know. I will respond as soon as possible (within 24 hours).  

We strive to provide superior customer service for you and make everything right from your point of view.

<#include "../include/footer.ftl">