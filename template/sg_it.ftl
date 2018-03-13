<#-- @ftlvariable name="order" type="edu.olivet.harvester.common.model.Order" -->
<#-- @ftlvariable name="config" type="edu.olivet.harvester.utils.Settings.Configuration" -->
<#-- @ftlvariable name="country" type="edu.olivet.foundations.amazon.Country" -->
Gentile ${order.recipient_name}, riguardo il suo ordine Amazon "${order.order_id}"
<--subject seperator-->
Gentile ${order.recipient_name},

Sono ${config.signature}, manager di ${config.storeName}.

Grazie per il tuo recente ordine di ${order.quantity_purchased} of ${order.item_name}. ID ordine: ${order.order_id}

Abbiamo spedito il prodotto per lei. Ma essendo stato spedito direttamente dal magazzino del fornitore, potrebbero volerci circa 3 mesi perché venga recapitato. Siamo davvero spiacenti per questa situazione inaspettata.

Comunque se per lei i tempi di spedizione sono troppo lunghi, non si preoccupi. Semplicemente risponda a questa email e ci faccia sapere, possiamo bloccare la spedizione ed emettere immediatamente un pieno rimborso.

Sono davvero spiacente dell'inconveniente causatole E' nostro errore non aver gestito bene le nostre scorte e migliorato la spedizione degli ordini. Capisco come si sente e mi sentirei allo stesso modo se non ricevessi in tempo il prodotto che ordino.

A volte capitano errori ed imprevisti, ma il perdono è divino. Nonostante non è possibile cambiare un fatto accaduto nel passato, perdonando, è possibile cambiare il significato del passato e far sì che la sua influenza verso il futuro sia migliore, dico bene?

Se possibile, può accettare le mie scuse? Vorrebbe inoltre cancellare l'ordine ed ottenere immediatamente un pieno rimborso?

Se ha altre domande, può ricontattarmi e farò del mio meglio per accontentarla.

Grazie per la comprensione e aspetto una risposta!


Cordialmente,
<#include "include/footer.ftl">