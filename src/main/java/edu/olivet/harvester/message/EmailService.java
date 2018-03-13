package edu.olivet.harvester.message;

import com.amazonaws.services.simpleemail.model.Destination;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.google.EmailContentType;
import edu.olivet.foundations.google.GmailSender;
import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.utils.Settings;
import lombok.Setter;
import org.apache.commons.mail.EmailException;

import java.io.File;

public class EmailService implements MessageService {

    @Inject
    private GmailSender gmailSender;

    @Setter
    protected Account senderAccount;

    @Setter
    protected boolean testMode = false;

    @Setter
    protected Destination testDestination = new Destination().withToAddresses(Constants.RND_EMAIL);

    @Inject
    public void init() {
        try {
            senderAccount = Settings.load().getConfigs().get(0).getSellerEmail();
        } catch (Exception e) {
            //
        }
    }

    public void sendMessage(String subject, String content, Destination destination) {
        if (senderAccount == null) {
            return;
        }
        try {
            if (testMode) {
                destination = testDestination;
            }
            gmailSender.send(senderAccount, destination, subject, content, EmailContentType.PlainText);
        } catch (EmailException e) {
            throw new BusinessException(e);
        }


    }


    public void sendMessage(Destination destination, String subject, String content,
                            EmailContentType contentType, File... attachments) {
        if (senderAccount == null) {
            return;
        }

        try {
            if (testMode) {
                destination = testDestination;
            }
            gmailSender.send(senderAccount, destination, subject, content, contentType, attachments);
        } catch (EmailException e) {
            throw new BusinessException(e);
        }


    }

}
