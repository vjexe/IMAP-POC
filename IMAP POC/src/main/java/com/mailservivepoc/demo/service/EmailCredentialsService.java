//package com.mailservivepoc.demo.service;
//
//import com.mailservivepoc.demo.dal.EmailCredentialsRepository;
//import com.mailservivepoc.demo.dto.EmailCredentialsDto;
//import com.mailservivepoc.demo.exceptions.EmailCredentialsNotFoundException;
//import com.mailservivepoc.demo.model.EmailCredentials;
//import jakarta.transaction.Transactional;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.crypto.Cipher;
//import javax.crypto.spec.SecretKeySpec;
//import java.util.Base64;
//import java.util.List;
//import java.util.Optional;
//
//
//@Service
//public class EmailCredentialsService {
//    private static final String ENCRYPTION_KEY = "C01D24F3A9B8E7615FED2D50E96CA7B5";
//
//    @Autowired
//    private EmailCredentialsRepository emailCredentialsRepository;
//
//
//
//
//    public EmailCredentials saveEmailCredentials(EmailCredentialsDto emailCredentialsDto) {
//        EmailCredentials emailCredentials = new EmailCredentials();
//        emailCredentials.setEmail(emailCredentialsDto.getEmail());
//        emailCredentials.setPassword(encrypt(emailCredentialsDto.getPassword()));
//        emailCredentials.setGroupId(emailCredentialsDto.getGroupId());
//        emailCredentials.setJobId(emailCredentialsDto.getJobId());
//        try {
//            return emailCredentialsRepository.save(emailCredentials);
//        } catch (Exception e) {
//            throw new RuntimeException("Error saving email credentials: " + e.getMessage());
//        }
//    }
//
//    public EmailCredentialsDto getEmailCredentialsByEmail(String email) {
//        Optional<EmailCredentials> optionalEmailCredentials = emailCredentialsRepository.findByEmail(email);
//        if (optionalEmailCredentials.isPresent()) {
//            EmailCredentials emailCredentials = optionalEmailCredentials.get();
//            return new EmailCredentialsDto(emailCredentials.getId(), emailCredentials.getEmail(),
//                    decrypt(emailCredentials.getPassword()), emailCredentials.getGroupId(), emailCredentials.getJobId());
//        }
//        throw new EmailCredentialsNotFoundException("Email credentials not found for email: " + email);
//    }
//
//    public List<EmailCredentialsDto> getAllEmailCredentials() {
//        try {
//            List<EmailCredentials> emailCredentialsList = emailCredentialsRepository.findAll();
//            return emailCredentialsList.stream()
//                    .map(ec -> new EmailCredentialsDto(ec, decrypt(ec.getPassword())))
//                    .toList();
//        } catch (Exception e) {
//            throw new RuntimeException("Error retrieving email credentials: " + e.getMessage());
//        }
//    }
//
//    public void updateEmailPassword(String email, String newPassword) {
//        Optional<EmailCredentials> optionalEmailCredentials = emailCredentialsRepository.findByEmail(email);
//        if (optionalEmailCredentials.isPresent()) {
//            EmailCredentials emailCredentials = optionalEmailCredentials.get();
//            emailCredentials.setPassword(encrypt(newPassword));
//            try {
//                emailCredentialsRepository.save(emailCredentials);
//            } catch (Exception e) {
//                throw new RuntimeException("Error updating email credentials: " + e.getMessage());
//            }
//        } else {
//            throw new EmailCredentialsNotFoundException("Email credentials not found for email: " + email);
//        }
//    }
//    @Transactional
//    public void deleteEmailCredentialsByEmail(String email) {
//        try {
//            emailCredentialsRepository.deleteByEmail(email);
//        } catch (Exception e) {
//            throw new RuntimeException("Error deleting email credentials: " + e.getMessage());
//        }
//    }
//
//    private String encrypt(String password) {
//        try {
//            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//            return Base64.getEncoder().encodeToString(cipher.doFinal(password.getBytes()));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private String decrypt(String password) {
//        try {
//            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//            cipher.init(Cipher.DECRYPT_MODE, key);
//            return new String(cipher.doFinal(Base64.getDecoder().decode(password)));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
//
//
