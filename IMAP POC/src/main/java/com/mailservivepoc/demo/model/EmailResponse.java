package com.mailservivepoc.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    private String subject;
    private String from;
    private String to;
    private Date sentDate;
    private String body;
    private List<Attachment> attachmentList;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Subject: ").append(subject).append("\n");
        sb.append("From: ").append(from).append("\n");
        sb.append("To: ").append(to).append("\n");
        sb.append("Sent Date: ").append(sentDate).append("\n");
        sb.append("Body: ").append(body).append("\n");
        sb.append("Attachments: ");
        if (attachmentList != null && attachmentList.size() > 0) {
            for (Attachment attachment : attachmentList) {
                sb.append(attachment.toString()).append(", ");
            }
            sb.deleteCharAt(sb.length() - 1); // remove last comma
            sb.deleteCharAt(sb.length() - 1); // remove last space
        } else {
            sb.append("none");
        }
        return sb.toString();
    }

}

