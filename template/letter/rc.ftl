<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
Hello ${order.recipient_name}, your order was refunded: (${order.order_id})
<--subject seperator-->
Dear ${order.recipient_name},
                         
We are contacting you regarding your order delivery status.  I know this is very disappointing but I have to let you know about the difficulty you are facing. 

As we prepared the shipment just now, we found the last item was just taken by another customer who placed her order just one moment earlier than yours. Upon realizing this issue, we contacted our supplier immediately to source a item for you, however they said the new stock is coming in at least 3 months.

We just issued the item price + shipping charge to your account. In this way you can get your money back asap so than you can purchase the item from someone else. Please allow 3-5 business days for the refund to complete to your account. 

I guess you might have received a shipment confirmation letter. This was caused by the miscommunication between our customer service and warehouse staff. The customer service staff confirmed the shipment in the IT system just moments before the warehouse staff notified them not to do so because the product was found out of stock. 

I sincerely apologize for any inconvenience this has caused to you. Please accept my apologies and let me know your thoughts.

<#include "../include/footer.ftl">