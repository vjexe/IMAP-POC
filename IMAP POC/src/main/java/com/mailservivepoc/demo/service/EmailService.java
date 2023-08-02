package com.mailservivepoc.demo.service;

import com.mailservivepoc.demo.model.*;
import com.sun.mail.imap.IMAPFolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;

import javax.mail.*;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.*;
import java.io.*;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmailService {


    private static final String IMAP_HOST = "imap.gmail.com";
    private static final String IMAP_PORT = "993";
    private static final String USERNAME = ""; // your username
    private static final String PASSWORD = ""; // your password
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public List<EmailResponse> getLatestMessage() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.put("mail.imap.partialfetch","false");
        properties.put("mail.imap.fetchsize", "1048576");
        //properties.put("mail.imaps.partialfetch", "false");
        //properties.put("mail.imaps.fetchsize", "1048576");
        properties.setProperty("mail.imap.ssl.enable", "true");

        Session session = Session.getInstance(properties);
        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);

        // to do - find method to fetch unread messages efficiently

        IMAPFolder folder = (IMAPFolder) store.getFolder("INBOX");
        folder.open(Folder.READ_WRITE);

        LocalDateTime filterDateTime = LocalDateTime.of(2023, 6, 21, 20, 24, 0);
        log.info("local date time is "+filterDateTime);


        // Convert LocalDateTime to Date
        Date filterDate = Date.from(filterDateTime.atZone(ZoneId.systemDefault()).toInstant());

        log.info("local date time after processing is " + filterDate);

// Fetch messages received until the filter date
        SearchTerm newerThanTerm = new ReceivedDateTerm(ComparisonTerm.GE, filterDate);
        FlagTerm unseenFlagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

// Combine the search terms using AndTerm
        SearchTerm searchTerm = new AndTerm(unseenFlagTerm, newerThanTerm);

        Message[] messages = folder.search(searchTerm);

// Filter messages based on the provided date and time
//        List<Message> filteredMessages = new ArrayList<>();
//        for (Message message : messages) {
//            Date receivedDate = message.getReceivedDate();
//            LocalDateTime messageDateTime = LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault());
//            if (messageDateTime.isAfter(filterDateTime)) {
//                filteredMessages.add(message);
//
//            }
//        }
//        Message[] messagesAfterTime = filteredMessages.toArray(new Message[0]);
        List<EmailResponse> emailResponseList = new ArrayList<>();

        for(Message mostRecent : messages){
            Date receivedDate = mostRecent.getReceivedDate();
            LocalDateTime messageDateTime = LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault());
            if (!messageDateTime.isAfter(filterDateTime)){
                continue;
            }
            EmailResponse emailResponse = new EmailResponse();


            processEmail(mostRecent);
            mostRecent.setFlag(Flags.Flag.SEEN, true);


        /*
        Flags seen = new Flags(Flags.Flag.SEEN);
        FlagTerm unseenFlagTerm = new FlagTerm(seen, false);
        Message[] messages = inbox.search(unseenFlagTerm);
        Message mostRecent = messages[messages.length-1];


        */

            // now let's check that this email is new email or it is a reply
            //processEmail(mostRecent);

            emailResponse.setSubject(mostRecent.getSubject());
            emailResponse.setFrom(Arrays.toString(mostRecent.getFrom()));
            emailResponse.setTo(Arrays.toString(mostRecent.getRecipients(Message.RecipientType.TO)));
            emailResponse.setSentDate(mostRecent.getSentDate());
            //logic to get the content of main body
            emailResponse.setBody(getTextFromMessage(mostRecent));
            emailResponse.setAttachmentList(getAttachments(mostRecent));
            //fetchAttachments(mostRecent);


            emailResponseList.add(emailResponse);

        /*

        //logic 1
         String body = "";
        if (mostRecent.isMimeType("text/plain")) {
            body += mostRecent.getContent().toString();
        } else if (mostRecent.isMimeType("multipart/*")) {
            Multipart multiPart = (Multipart) mostRecent.getContent();
            for (int i = 0; i < multiPart.getCount(); i++) {
                BodyPart bodyPart = multiPart.getBodyPart(i);
                System.out.println(bodyPart.getContent().toString());
                if (bodyPart.isMimeType("text/plain")) {
                    body += bodyPart.getContent().toString();
                }
            }
        }

       // logic 2
        Object content = mostRecent.getContent();
        if (content instanceof String) {
            // handle text/plain content
            body += content.toString();
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain")) {
                    // handle text/plain body part
                    body+= bodyPart.getContent().toString();
                    break;
                } else if (bodyPart.isMimeType("text/html")) {
                    // handle text/html body part
                    body+= bodyPart.getContent().toString();
                    break;
                }
            }
        }
        */

        }



        return emailResponseList;
    }

    public EmailReplyResponse replyToLastEmail(@RequestBody EmailReplyRequest emailReply) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(properties);


        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        Message[] messages = inbox.getMessages();
        System.out.println(messages.length);
        if (messages.length == 0) {
            throw new Exception("Inbox is empty");
        }
        Message message = messages[messages.length - 1];

        EmailReplyResponse response = new EmailReplyResponse();


        Date date = message.getSentDate();
        response.setSentDate(date);
        // Get all the information from the message
        String from = InternetAddress.toString(message.getFrom());
        response.setReplyingTo(from);

        //String replyTo = InternetAddress.toString(message.getReplyTo());


        //String to = InternetAddress.toString(message.getRecipients(Message.RecipientType.TO));


        String subject = message.getSubject();
        response.setSubject(subject);

        response.setBody(emailReply.getBody());

        Message replyMessage = new MimeMessage(session);
        replyMessage = (MimeMessage) message.reply(true);
        //replyMessage.setFrom(new InternetAddress(to));
        replyMessage.setText(emailReply.getBody());
        //replyMessage.setReplyTo(message.getReplyTo());

        Transport t = session.getTransport("smtp");
        t.connect(USERNAME, PASSWORD);
        t.sendMessage(replyMessage,replyMessage.getAllRecipients());
        t.close();
        System.out.println("message replied successfully ....");

        return response;
    }
    public EmailReplyResponse replyToLastEmailIncludePrevious(@RequestBody EmailReplyRequest emailReply) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(properties);

        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        Message[] messages = inbox.getMessages();
        System.out.println(messages.length);
        if (messages.length == 0) {
            throw new Exception("Inbox is empty");
        }
        Message message = messages[messages.length - 1];

        EmailReplyResponse response = new EmailReplyResponse();

        Date date = message.getSentDate();
        response.setSentDate(date);

        String from = InternetAddress.toString(message.getFrom());
        response.setReplyingTo(from);

        String subject = message.getSubject();
        response.setSubject(subject);

        // Create the reply message
        Message replyMessage = new MimeMessage(session);
        replyMessage = (MimeMessage) message.reply(true);

        // Set the reply message content
        Multipart multipart = new MimeMultipart();
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(emailReply.getBody());

        // Include the original message content in the reply
        MimeBodyPart originalMessagePart = new MimeBodyPart();
        originalMessagePart.setContent(message, "message/rfc822");
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(originalMessagePart);

        replyMessage.setContent(multipart);

        // Send the reply message
        Transport t = session.getTransport("smtp");
        t.connect(USERNAME, PASSWORD);
        t.sendMessage(replyMessage, replyMessage.getAllRecipients());
        t.close();
        System.out.println("Message replied successfully....");

        return response;
    }




    public void sendEmail(@RequestBody EmailRequest emailRequest) throws Exception {
        Properties props = new Properties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "true");
        props.setProperty("mail.smtp.host", SMTP_HOST);
        props.setProperty("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        MimeMessage message = new CustomMimeMessage(session);
        message.setFrom(new InternetAddress(USERNAME));
        message.setReplyTo(InternetAddress.parse("vaibhav.joshi4@byjus.com"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(emailRequest.getTo()));
        message.setSubject(emailRequest.getSubject());
        message.setText(emailRequest.getBody());

        // Set custom message ID
        String messageId = "<" + UUID.randomUUID().toString() + "@" + SMTP_HOST + ">";
        System.out.println(messageId);
        message.setHeader("Message-ID", messageId);

        Transport.send(message);
    }

    public class CustomMimeMessage extends MimeMessage {
        public CustomMimeMessage(Session session) {
            super(session);
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            // do nothing to override default message ID generation
        }
    }



    public void processEmail(Message message) throws MessagingException {
        //String messageId = message.getHeader("Message-ID")[0];
        String[] inReplyTo = message.getHeader("In-Reply-To");
        String[] references = message.getHeader("References");

        if (references != null){
            System.out.println("this is the reference" + references[0]);
            System.out.println("   ");
            String referencesString = !Objects.isNull(references) ? references[0] : (Objects.isNull(inReplyTo)? null: inReplyTo[0]);
            String parentMessageId = referencesString.split("\r\n")[0];
            ArrayList<String> referencesList = new ArrayList<>(Arrays.asList(referencesString.split("\r\n")));
            // Trim each element in referencesList to remove leading and trailing spaces
            List<String> trimmedList = referencesList.stream()
                    .map(String::trim)
                    .collect(Collectors.toList());

            System.out.println(referencesList);
            System.out.println("trimmed list is : " + trimmedList);
            // This is a reply to a previous email

        }

        if (inReplyTo != null){
            System.out.println("this is the reference" + inReplyTo[0]);
            System.out.println("   ");

        }

        // how do you know the ticket id for which this reply is

        if (inReplyTo != null || references != null) {
            // This is a reply to a previous email
            String parentMessageId = inReplyTo != null ? inReplyTo[0] : references[0];
            System.out.println("this is the mail received as a reply");
            // Do something with the parent message ID
        } else {
            // This is a new email
            System.out.println("this is a new email");
            // Do something with the new email
        }
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String content = getTextFromPart(bodyPart);
                result = result + content;
            }
        }
        return result;
    }


    private String getTextFromPart(BodyPart bodyPart) throws MessagingException, IOException {
        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            try {
                result = (String) bodyPart.getContent();
            } catch (ClassCastException e) {
                Object content = bodyPart.getContent();
                if (content instanceof InputStream) {
                    InputStream is = (InputStream) content;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    result = sb.toString();
                }
            }
        } else if (bodyPart.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) bodyPart.getContent();
            int count = multipart.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart nestedBodyPart = multipart.getBodyPart(i);
                String content = getTextFromPart(nestedBodyPart);
                result = result + content;
            }
        }
        return result;
    }


    public List<Attachment> getAttachments(Message message) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();
//        Object content = message.getContent();
//        if (content instanceof Multipart) {
//            Multipart multipart = (Multipart) content;
//
//            for (int i = 0; i < multipart.getCount(); i++) {
//                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
//                String disposition = bodyPart.getDisposition();
//
//                if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
//                    // Process the attachment or inline part
//                    String fileName = bodyPart.getFileName();
//                    // Do something with the attachment or inline part
//                    System.out.println("Found attachment or inline part: " + fileName);
//                }
//            }
//        }

        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart bodyPart =  (MimeBodyPart) multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();
                if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
                    String fileName = bodyPart.getFileName();
                    String contentType = bodyPart.getContentType();
                    long size = bodyPart.getSize();
                    log.info("the size of the file is {}:", size);
                    uploadAttachment(bodyPart);
                    // create an attachment object and add it to the list of attachments
                    Attachment attachment = new Attachment(fileName, contentType, size);
                    attachments.add(attachment);
                }else if (bodyPart.getContent() instanceof Multipart) {
                    // Handle nested multipart content
                    Multipart nestedMultipart = (Multipart) bodyPart.getContent();

                    for (int j = 0; j < nestedMultipart.getCount(); j++) {
                        MimeBodyPart nestedBodyPart = (MimeBodyPart) nestedMultipart.getBodyPart(j);
                        String nestedDisposition = nestedBodyPart.getDisposition();

                        if (nestedDisposition != null && (nestedDisposition.equalsIgnoreCase(Part.INLINE)|| nestedDisposition.equalsIgnoreCase(Part.ATTACHMENT))) {
                            // Process the inline part
                            String inlineFileName = nestedBodyPart.getFileName();
                            // Do something with the inline part
                            String fileName = nestedBodyPart.getFileName();
                            String contentType = nestedBodyPart.getContentType();
                            long size = nestedBodyPart.getSize();
                            log.info("the size of the file is {}:", size);
                            uploadAttachment(nestedBodyPart);
                            // create an attachment object and add it to the list of attachments
                            Attachment attachment = new Attachment(fileName, contentType, size);
                            attachments.add(attachment);
                            //System.out.println("Found inline part: " + inlineFileName);
                        }
                    }
                }
            }
        }
        return attachments;
    }
    private static void fetchAttachments(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;

            for (int i = 0; i < multipart.getCount(); i++) {
                MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();

                if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
                    // Process the attachment part
                    String fileName = bodyPart.getFileName();
                    // Do something with the attachment
                    System.out.println("Found attachment: " + fileName);
                } else if (bodyPart.getContent() instanceof Multipart) {
                    // Handle nested multipart content
                    Multipart nestedMultipart = (Multipart) bodyPart.getContent();

                    for (int j = 0; j < nestedMultipart.getCount(); j++) {
                        MimeBodyPart nestedBodyPart = (MimeBodyPart) nestedMultipart.getBodyPart(j);
                        String nestedDisposition = nestedBodyPart.getDisposition();

                        if (nestedDisposition != null && nestedDisposition.equalsIgnoreCase(Part.INLINE)) {
                            // Process the inline part
                            String inlineFileName = nestedBodyPart.getFileName();
                            // Do something with the inline part
                            System.out.println("Found inline part: " + inlineFileName);
                        }
                    }
                }
            }
        }
    }
    public void uploadAttachment(MimeBodyPart bodyPart){
        UUID uuid = UUID.randomUUID();
        try{

            // Get the attachment file name
            String fileName = bodyPart.getFileName();


            // Create a ByteArrayOutputStream to read the content from the BodyPart's input stream
            //
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            long startTime3 = System.currentTimeMillis();
            InputStream inputStream = bodyPart.getInputStream();
            long endTime3 = System.currentTimeMillis();
            long elapsedTime3 = endTime3 - startTime3;
            log.info("{} time taken to fetch InputStream: {}", uuid.toString(), elapsedTime3);
            byte[] buffer = new byte[131072];
            int bytesRead;
            long startTime = System.currentTimeMillis();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byteArrayOutputStream.close();
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            log.info("{} time taken to write into byteArrayOutputStream: {}", uuid.toString(), elapsedTime);

            // Get the byte array of the attachment's content
            long startTime1 = System.currentTimeMillis();
            byte[] content = byteArrayOutputStream.toByteArray();
            long endTime1 = System.currentTimeMillis();
            long elapsedTime1 = endTime1 - startTime1;
            log.info("{} time taken to convert byteArrayOutputStream into content array: {}", uuid.toString(), elapsedTime1);

            // Create the request body for the file upload
            MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("file", new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });



        }catch(Exception e){

        }
    }


    public void listenForNewMessages() throws Exception {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");

        Session session = Session.getInstance(properties);
        Store store = session.getStore("imap");
        store.connect(USERNAME, PASSWORD);

        IMAPFolder inbox = (IMAPFolder) store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);


        Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
        for (Message message : unreadMessages) {
            EmailResponse emailResponse = new EmailResponse();

            emailResponse.setSubject(message.getSubject());
            emailResponse.setFrom(Arrays.toString(message.getFrom()));
            emailResponse.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
            emailResponse.setSentDate(message.getSentDate());
            emailResponse.setBody(getTextFromMessage(message));
            emailResponse.setAttachmentList(getAttachments(message));

            // do something with the email response, such as forwarding it to another email address
            System.out.println(emailResponse.toString());

            message.setFlag(Flags.Flag.SEEN, true);
        }


        inbox.addMessageCountListener(new MessageCountAdapter() {
            public void messagesAdded(MessageCountEvent ev) {
                Message[] messages = ev.getMessages();
                for (Message message : messages) {
                    try {
                        EmailResponse emailResponse = new EmailResponse();

                        emailResponse.setSubject(message.getSubject());
                        emailResponse.setFrom(Arrays.toString(message.getFrom()));
                        emailResponse.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
                        emailResponse.setSentDate(message.getSentDate());
                        emailResponse.setBody(getTextFromMessage(message));
                        emailResponse.setAttachmentList(getAttachments(message));

                        // do something with the email response, such as forwarding it to another email address
                        System.out.println(emailResponse.toString());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        idle(inbox);
    }


    private void idle(IMAPFolder folder) {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (5 * 60 * 1000); // 5 minutes
            while (System.currentTimeMillis() < endTime) {
                if (!folder.isOpen()) {
                    return;
                }
                folder.idle();
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void replyToEmail(String messageId) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imap");
        properties.put("mail.imap.host", IMAP_HOST);
        properties.put("mail.imap.port", IMAP_PORT);
        properties.setProperty("mail.imap.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);
        Session session = Session.getInstance(properties);

        // Connect to the IMAP server and open the inbox folder
        Store store = session.getStore();
        store.connect(IMAP_HOST, USERNAME, PASSWORD);
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        // Create a search term to search for the email with the specified Message-ID
        SearchTerm searchTerm = new MessageIDTerm(messageId);

        // Search for the email with the specified Message-ID
        Message[] messages = inbox.search(searchTerm);

        if (messages.length == 1) {
            // The email with the specified Message-ID was found
            Message originalMessage = messages[0];

            // Create a reply message
            Message replyMessage = new MimeMessage(session);
            replyMessage = (MimeMessage) originalMessage.reply(false); // This creates a reply to the original message without including its content
            replyMessage.setFrom(new InternetAddress(USERNAME));
            replyMessage.setText("This is my reply text.");
            Transport t = session.getTransport("smtp");
            t.connect(USERNAME, PASSWORD);
            t.sendMessage(replyMessage,replyMessage.getAllRecipients());
            t.close();
            System.out.println("message replied successfully ....");

        } else {
            // No email with the specified Message-ID was found
            System.out.println("Email with Message-ID " + messageId + " not found in inbox.");
        }

        // Close the inbox and store
        inbox.close(false);
        store.close();
    }
    public static void sendEmailInBulk() {
        // SMTP server details
        String smtpHost = "smtp.gmail.com";
        int smtpPort = 587;
        String username = "vjexe007@gmail.com";
        String password = "rahrvhhkrdtwojre";

        // Sender and recipient details
        String senderEmail = "vjexe007@gmail.com";
        String recipientEmail = "support-wfms@byjus.com";

        // Email content

        // Number of emails to send
        int numEmails = 10;

        // Set SMTP properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        // Create session with authentication
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Create a fixed thread pool with a maximum of 10 threads
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        try {
            for (int i = 0; i < numEmails; i++) {
                String subject = "Sample Subject : " + i;
                String body = "This is the content of the email. : " + i;

                // Create a new message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(body);

                // Send the message asynchronously using a thread from the thread pool
                executorService.execute(new EmailTask(message, i));
            }

            // Shut down the thread pool after all tasks have been submitted
            executorService.shutdown();

            // Wait for all tasks to complete
            while (!executorService.isTerminated()) {
                // Optional: You can add a delay here if needed
            }

            System.out.println(numEmails + " emails sent successfully!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // Runnable task for sending an email
    private static class EmailTask implements Runnable {
        private final Message message;
        private final int emailIndex;

        public EmailTask(Message message, int emailIndex) {
            this.message = message;
            this.emailIndex = emailIndex;
        }

        @Override
        public void run() {
            try {
                // Send the message
                Transport.send(message);
                log.info( "Email {0} sent successfully", emailIndex);
            } catch (MessagingException e) {
                log.error("Error sending email " + emailIndex, e);
            }
        }
    }




}







