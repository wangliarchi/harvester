package edu.olivet.harvester.message;

import com.amazonaws.services.simpleemail.model.Destination;
import edu.olivet.foundations.google.EmailContentType;

import java.io.File;

public interface MessageService {

    public boolean sendMessage(String subject, String content, Destination destination);
    boolean sendMessage(Destination destination, String subject, String content,
                        EmailContentType contentType, File... attachments);

}
