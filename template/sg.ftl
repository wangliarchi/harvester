<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
Dear ${order.recipient_name}, regarding your Amazon order "${order.order_id}"
<--subject seperator-->
Dear ${order.recipient_name},

This is ${config.userCode}, the store manager of ${config.storeName}.

Thank you for your recent purchase ${order.quantity_purchased} of ${order.item_name} from us. Order ID:  ${order.order_id}

We have shipped the item out for you. But as it was shipped out directly from our supplier's warehouse, it would take around 3 months to arrive. We are really sorry for the unexpected situation.

However, if the delivery frame is too late for you, please do not worry. Just reply to this email and let us know, we will stop your package and issue you a full refund immediately.

I am really very sorry for all the inconvenience it will cause to you. It's totally our fault that we should have managed our stocks well and improved the delivery of the orders. I truly understand your feeling and I would feel the same if I didn't get my expected item in time.

Mistakes and unexpected things sometimes may happen, but to forgive is divine. Though we could not change the fact that happened in the past, however, by forgiving, the meaning of the past and its influence towards the future could be changed for the better, isn't it?

If it is possible, would you mind accepting my apology? And I am wondering if you would like to cancel the order and receive the full refund immediately?

If you have any more questions, please feel free to contact me back and I would try my best to make everything right for you.

Thank you for understanding and waiting for your reply!


Sincerely,
<#include "include/footer.ftl">