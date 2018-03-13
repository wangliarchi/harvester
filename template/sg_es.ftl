<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
<#-- @ftlvariable name="country" type="edu.olivet.foundations.amazon.Country" -->
Estimado ${order.recipient_name}, acerca de su orden de Amazon "${order.order_id}"
<--subject seperator-->
Querido ${order.recipient_name},

Soy ${config.signature}, la gerente de la tienda de ${config.storeName}.

Gracias por su compra reciente ${order.quantity_purchased} of "${order.item_name}". pedido ID:${order.order_id}

Hemos procesado el envio de su articulo, pero como acabamos de enterar de nuestro encargado del almacen del surtidor, hay algunas complicaciones y tomara alrededor 3 meses para que su articulo sea entregado.

Lo sentimos mucho para esta situacion inesperada. Por favor, acepte nuestras disculpas por cualquier incovenencia que esto le haya causado. Estamos trabajando duro para mejorar continuamente nuestros servicios y evitar problemas como este en el futuro.

Si este nuevo plazo de entrega es demasiado largo para usted, responda a este correo electronico y haganos saber si desea cancelar el pedido. Podemos detener su paquete y emitir un reembolso completo inmediatamente.

Si usted tiene cualesquiera pregunta, por favor pongase en contacto con nosotros y haremos nuestro mejor para asistirle.

Gracias por entender,


Sinceramente,
<#include "include/footer.ftl">