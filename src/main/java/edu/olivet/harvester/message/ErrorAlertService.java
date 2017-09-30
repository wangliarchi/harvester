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

public class ErrorAlertService extends EmailService {

    @Setter
    private Destination destination = new Destination().withToAddresses("johnnyxiang2017@gmail.com");



    public void sendMessage(String subject, String content)  {

        subject = Settings.load().getSid() + subject;
        super.sendMessage(subject,content,destination);
    }


    public void sendMessage(String subject, String content, File... attachments) {
       super.sendMessage(destination,subject,content,EmailContentType.Html,attachments);
    }

}
