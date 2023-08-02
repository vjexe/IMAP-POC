//package com.mailservivepoc.demo.controller;
//
//import com.mailservivepoc.demo.dto.EmailCredentialsDto;
//import com.mailservivepoc.demo.exceptions.EmailCredentialsNotFoundException;
//import com.mailservivepoc.demo.model.EmailCredentials;
//import com.mailservivepoc.demo.service.EmailCredentialsService;
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.NotBlank;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.BindingResult;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@RestController
//@Validated
//@RequestMapping("/wfms/emailCredentials/v1")
//public class EmailCredentialsController {
//    @Autowired
//    private EmailCredentialsService emailCredentialsService;
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> handleException(RuntimeException e) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//    }
//
//    @PostMapping("/credential")
//    public ResponseEntity<?> saveEmailCredentials(@Valid @RequestBody EmailCredentialsDto emailCredentialsDto, BindingResult result) {
//
//        if(result.hasErrors()) {
//            List<String> errors = result.getFieldErrors()
//                    .stream()
//                    .map(err -> err.getDefaultMessage())
//                    .collect(Collectors.toList());
//            return ResponseEntity.badRequest().body(errors);
//        }
//
//        EmailCredentials savedEmailCredentials = emailCredentialsService.saveEmailCredentials(emailCredentialsDto);
//        return ResponseEntity.ok(new EmailCredentialsDto(savedEmailCredentials.getId(), savedEmailCredentials.getEmail(),
//                null, savedEmailCredentials.getGroupId(), savedEmailCredentials.getJobId()));
//    }
//
//
//    @GetMapping("/credential/{email}")
//    public ResponseEntity<EmailCredentialsDto> getEmailCredentialsByEmail(@PathVariable String email) {
//        try {
//            EmailCredentialsDto emailCredentialsDto = emailCredentialsService.getEmailCredentialsByEmail(email);
//            return ResponseEntity.ok(emailCredentialsDto);
//        } catch (EmailCredentialsNotFoundException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @GetMapping("/getAllCredentials")
//    public List<EmailCredentialsDto> getAllEmailCredentials() {
//        return emailCredentialsService.getAllEmailCredentials();
//    }
//
//    @PutMapping("/credential/{email}")
//    public void updateEmailPassword(@PathVariable String email, @RequestParam @NotBlank(message = "New password is required") String newPassword) {
//        emailCredentialsService.updateEmailPassword(email, newPassword);
//    }
//
//    @DeleteMapping("/credential/{email}")
//    public void deleteEmailCredentialsByEmail(@PathVariable String email) {
//        emailCredentialsService.deleteEmailCredentialsByEmail(email);
//    }
//
//}
//
//
