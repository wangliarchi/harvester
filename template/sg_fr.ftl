<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
<#-- @ftlvariable name="country" type="edu.olivet.foundations.amazon.Country" -->
Bonjour ${order.recipient_name}, à propos de votre commande Amazon "${order.order_id}"
<--subject seperator-->
Cher ${order.recipient_name},

Je suis ${config.signature}, gérant de la boutique ${config.storeName}. Je vous remercie pour votre achat de ${order.quantity_purchased} of "${order.item_name}" chez nous. Référence de commande: ${order.order_id}

Nous avons livré la commande à votre adresse. Mais comme elle a été livrée directement depuis l'entrepôt de notre fournisseur, elle pourrait prendre trois mois pour arriver. Nous sommes vraiment désolés pour cette situation inattendue.

Toutefois, si la date de livraison est trop tardive pour vous, ne vous inquiétez pas. Vous n'avez qu'à répondre à cet e-mail et nous le faire savoir, alors nous interromprons la commande et nous vous rembourserons immédiatement. Je suis vraiment désolé pour tous les problèmes que cela peut vous causer. C'est vraiment mieux, nous aurions du mieux gérer nos stocks et améliorer la livraison des commandes. Je comprends vraiment votre déception et je penserais la même chose que vous si je ne recevais pas ma commande à temps.

Les erreurs et les imprévus peut parfois arriver, mais pardonner, c'est divin. Bien que nous ne pouvons pas changer ce qui est arrivé dans le passé, en pardonnant, la signification du passé et son influence envers le futur peut être changée en mieux, n'est-ce pas?

S'il est possible, accepteriez-vous mes excuses? Et je vous demande, voudriez-vous annuler votre commande et recevoir un remboursement intégral immédiatement?

Si vous avez plus de questions, sentez-vous libre de me contacter et je ferai de mon mieux pour régler la situation pour vous.

Merci pour votre compréhension, j'attend votre réponse!

Sincères salutations,
<#include "include/footer.ftl">