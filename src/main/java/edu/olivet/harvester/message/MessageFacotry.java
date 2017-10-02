package edu.olivet.harvester.message;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/30/17 9:16 AM
 */
public class MessageFacotry {

    public enum MessageType {
        Email
    }

    public static MessageService getService(MessageType type) {
        if (type == MessageType.Email) {
            return new EmailService();
        }

        return new EmailService();
    }
}
