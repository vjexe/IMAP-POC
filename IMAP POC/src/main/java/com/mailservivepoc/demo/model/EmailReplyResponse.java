package com.mailservivepoc.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmailReplyResponse {
    private String subject;
    private String replyingTo;
    private Date sentDate;
    private String body;

}
