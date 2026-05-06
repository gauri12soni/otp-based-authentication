////package com.gauri.otpBasedAuthentication.service;
////
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.beans.factory.annotation.Value;
////import org.springframework.http.*;
////import org.springframework.stereotype.Service;
////import org.springframework.web.client.RestTemplate;
////
////import java.util.Map;
////
////@Service
////@Slf4j
////@RequiredArgsConstructor
////public class SmsService {
////
////    @Value("${msg91.auth.key}")
////    private String authKey;
////
////    @Value("${msg91.template.id}")
////    private String templateId;
////
////    @Value("${msg91.api.url}")
////    private String apiUrl;
////
////    private final RestTemplate restTemplate;
////
////
////    public boolean sendOtp(String phone, String otp) {
////        try {
////            // Remove '+' from phone if present for MSG91
////            String mobile = phone.replace("+", "");
////
////            HttpHeaders headers = new HttpHeaders();
////            headers.setContentType(MediaType.APPLICATION_JSON);
////            headers.set("authkey", authKey);
////
////            // MSG91 v5 OTP API request body
////            Map<String, String> body = Map.of(
////                    "template_id", templateId,
////                    "mobile", mobile,
////                    "otp", otp
////            );
////
////            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
////            ResponseEntity<Map> response = restTemplate.exchange(
////                    apiUrl,
////                    HttpMethod.POST,
////                    entity,
////                    Map.class
////            );
////
////            log.info("MSG91 Response for {} : {}", mobile, response.getBody());
////            // MSG91 returns {"type":"success"} on success
////            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
////                String type = (String) response.getBody().get("type");
////                if (!"success".equals(type)) {
////                    log.warn("MSG91 returned non-success for {}: {}", mobile, response.getBody());
////                }
////                return "success".equals(type);
////            }
////
////            log.warn("MSG91 unexpected status {} for {}", response.getStatusCode(), mobile);
////
////            return false;
////        } catch (Exception e) {
////            log.error("Failed to send OTP via MSG91 for phone {} : {}", phone, e.getMessage());
////            return false;
////        }
////    }
////}
//
//
//package com.gauri.otpBasedAuthentication.service;
//
//import com.twilio.Twilio;
//import com.twilio.rest.api.v2010.account.Message;
//import com.twilio.type.PhoneNumber;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//@Service
//@Slf4j
//public class SmsService {
//
//    @Value("${twilio.account.sid}")
//    private String accountSid;
//
//    @Value("${twilio.auth.token}")
//    private String authToken;
//
//    @Value("${twilio.phone.number}")
//    private String fromNumber;
//
//    // Initializes Twilio SDK once when Spring creates this bean
//    @PostConstruct
//    public void init() {
//        Twilio.init(accountSid, authToken);
//        log.info("Twilio initialized successfully");
//    }
//
//    public boolean sendOtp(String phone, String otp) {
//        try {
//            Message message = Message.creator(
//                    new PhoneNumber(phone),        // to — user's number e.g. +919876543210
//                    new PhoneNumber(fromNumber),   // from — your Twilio number
//                    "Your OTP is " + otp + ". Valid for 10 minutes. Do not share with anyone."
//            ).create();
//
//            log.info("Twilio SMS sent to {} | SID: {} | Status: {}",
//                    phone, message.getSid(), message.getStatus());
//
//            // Twilio status is "queued" or "sent" on success
//            return message.getSid() != null;
//
//        } catch (Exception e) {
//            log.error("Twilio SMS failed for {}: {}", phone, e.getMessage());
//            return false;
//        }
//    }
//}