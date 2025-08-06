package com.meobeo.truyen.service.impl;

import com.meobeo.truyen.service.interfaces.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final SendGrid sendGrid;

    @Value("${sendgrid.template.otp}")
    private String otpTemplateId;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            Mail mail = new Mail();
            mail.setFrom(new Email("noreply@meobeo.com", "Tiệm Truyện Mèo Béo"));
            mail.setTemplateId(otpTemplateId);

            Personalization personalization = new Personalization();
            personalization.addTo(new Email(toEmail));
            personalization.addDynamicTemplateData("otpCode", otpCode);

            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Gửi email OTP thành công đến: {}", toEmail);
            } else {
                log.error("Lỗi gửi email OTP đến {}: {}", toEmail, response.getBody());
                throw new RuntimeException("Không thể gửi email OTP");
            }

        } catch (IOException e) {
            log.error("Lỗi gửi email OTP đến {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Không thể gửi email OTP", e);
        }
    }

}