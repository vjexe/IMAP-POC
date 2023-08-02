//package com.mailservivepoc.demo.dto;
//
//import com.mailservivepoc.demo.model.EmailCredentials;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import lombok.*;
//
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Getter
//@Setter
//public class EmailCredentialsDto {
//    private Long id;
//    @NotBlank(message = "Email is required")
//    @Email(regexp = "[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,3}",
//            flags = Pattern.Flag.CASE_INSENSITIVE,message = "Email format is invalid")
//    private String email;
//
//    @NotBlank(message = "Password is required")
//    private String password;
//    private Integer groupId;
//    private String jobId;
//
//    public EmailCredentialsDto(EmailCredentials ec, String password) {
//        this.id = ec.getId();
//        this.email = ec.getEmail();
//        this.password = password;
//        this.groupId = ec.getGroupId();
//        this.jobId = ec.getJobId();
//    }
//
//
//}
