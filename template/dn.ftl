<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
<#assign DateUtils=statics['org.apache.commons.lang3.time.DateUtils']>
Dear ${order.recipient_name}, regarding your Amazon order "${order.order_id}"
<--subject seperator-->
Dear ${order.recipient_name},

Hope you are doing the best.

This is ${config.signature}, the manager from ${config.storeName} on Amazon marketplace, where you purchased ${order.item_name} on ${order.purchase_date}. Order ID: ${order.order_id}

I am so sorry to tell you that we got an urgent delivery delay notice from our carrier today. The delivery of many packages will be delayed for 10-15 business days. (You will receive your package in 10-15 business days after the Amazon official estimated delivery due date.)

We are proactively contacting the carrier to get further information. Please do believe we didn't mean it and such kind of issue was out of our control.

At this point, would you like to wait until ${DateUtils.addDays(order.latestEdd(),10)?date} (10 more business days after the EDD)? Or, we can issue you a full refund right now immediately (And when the item arrives, you can either send us the refund back or return it to us).

I am wondering which solution do you prefer?

I apologize for all the inconvenience.

Hope to hear from you soon.

Sincerely,
<#include "include/footer.ftl">