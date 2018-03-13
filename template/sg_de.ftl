<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
<#-- @ftlvariable name="country" type="edu.olivet.foundations.amazon.Country" -->
Lieber ${order.recipient_name}, Ihre Amazon Bestellung "${order.order_id}"
<--subject seperator-->
Mein Name ist ${config.signature}, der Manager von ${config.storeName} auf Amazon-Marktplatz.

Vielen Dank für Ihren Einkauf von ${order.quantity_purchased} of ${order.item_name}. Ihre Bestellnummer: ${order.order_id}

Ihr Artikel wurde schon verschikt, aber da es dirket von unserem Lieferanten-Lagerhaus versendet wurde, dauert es etwa 3 Moante bei Ihnen anzukommen. Es tut uns sehr für die unerwartete Situation leid.

Wenn die Lieferzeit für Sie zu spät ist, dann brauchen Sie keine Sorgen zu machen. Antworten Sie einfach auf diese Email und lassen Sie es uns wissen. Wir werden Ihr Paket stoppen und Ihnen eine volle Rückerstattung veranlassen.

Es ist wirklich unsere Schuld, dass wir unsere Bestände nicht gut verwaltet haben. Für diese Unannehmlichkeiten möchte ich nochmal bei Ihnen entschuldigen. Ich verstehe sehr gut wie Sie sich fühlen werden, weil ich auch genauso fühlen werde, wenn mein erwartetes Paket nicht im versprochenen Zeitraum ankommt.

Würden Sie meine Entschuldigung akzeptieren und volle Rückerstattung bekommen, indem Sie die Bestellung stornieren? Wenn Sie weitere Fragen haben, wenden Sie sich bitte an mich zurück und ich werde mein Bestes geben um alles richtig zu machen.

Vielen Dank für Ihr Verständnis und ich freue mich auf Ihre Rückmeldung!


Mit freundlichen Grüssen
<#include "include/footer.ftl">