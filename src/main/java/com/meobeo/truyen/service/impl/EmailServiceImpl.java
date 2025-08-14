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

    @Value("${sendgrid.template.forgot-password}")
    private String forgotPasswordTemplateId;

    @Value("${sendgrid.template.topup-success}")
    private String topupSuccessTemplateId;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("Bắt đầu gửi email OTP đến: {}", toEmail);

        try {
            Mail mail = new Mail();
            mail.setFrom(new Email("noreply.meobeo@gmail.com", "Tiệm Truyện Mèo Béo"));

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
                String errorMessage = String.format("SendGrid API trả về lỗi. Status: %d, Body: %s",
                        response.getStatusCode(), response.getBody());
                log.error("Lỗi gửi email OTP đến {}: {}", toEmail, errorMessage);
                throw new RuntimeException(errorMessage);
            }

        } catch (IOException e) {
            String errorMessage = String.format("Lỗi kết nối SendGrid: %s", e.getMessage());
            log.error("Lỗi gửi email OTP đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format("Lỗi không xác định khi gửi email: %s", e.getMessage());
            log.error("Lỗi gửi email OTP đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void sendForgotPasswordEmail(String toEmail, String otpCode) {
        log.info("Bắt đầu gửi email quên mật khẩu đến: {}", toEmail);

        try {
            Mail mail = new Mail();
            mail.setFrom(new Email("noreply.meobeo@gmail.com", "Tiệm Truyện Mèo Béo"));

            mail.setTemplateId(forgotPasswordTemplateId);

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
                log.info("Gửi email quên mật khẩu thành công đến: {}", toEmail);
            } else {
                String errorMessage = String.format("SendGrid API trả về lỗi. Status: %d, Body: %s",
                        response.getStatusCode(), response.getBody());
                log.error("Lỗi gửi email quên mật khẩu đến {}: {}", toEmail, errorMessage);
                throw new RuntimeException(errorMessage);
            }

        } catch (IOException e) {
            String errorMessage = String.format("Lỗi kết nối SendGrid: %s", e.getMessage());
            log.error("Lỗi gửi email quên mật khẩu đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format("Lỗi không xác định khi gửi email: %s", e.getMessage());
            log.error("Lỗi gửi email quên mật khẩu đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

    @Override
    public void sendTopupSuccessEmail(String toEmail, String userName, String packageName,
            String amount, String newBalance, String time, String walletUrl) {
        log.info("Bắt đầu gửi email thông báo nạp tiền thành công đến: {}", toEmail);

        try {
            Mail mail = new Mail();
            mail.setFrom(new Email("noreply.meobeo@gmail.com", "Tiệm Truyện Mèo Béo"));

            mail.setTemplateId(topupSuccessTemplateId);

            Personalization personalization = new Personalization();
            personalization.addTo(new Email(toEmail));
            personalization.addDynamicTemplateData("userName", userName);
            personalization.addDynamicTemplateData("packageName", packageName);
            personalization.addDynamicTemplateData("amount", amount);
            personalization.addDynamicTemplateData("newBalance", newBalance);
            personalization.addDynamicTemplateData("time", time);
            personalization.addDynamicTemplateData("walletUrl", walletUrl);

            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Gửi email thông báo nạp tiền thành công đến: {}", toEmail);
            } else {
                String errorMessage = String.format("SendGrid API trả về lỗi. Status: %d, Body: %s",
                        response.getStatusCode(), response.getBody());
                log.error("Lỗi gửi email thông báo nạp tiền thành công đến {}: {}", toEmail, errorMessage);
                throw new RuntimeException(errorMessage);
            }

        } catch (IOException e) {
            String errorMessage = String.format("Lỗi kết nối SendGrid: %s", e.getMessage());
            log.error("Lỗi gửi email thông báo nạp tiền thành công đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format("Lỗi không xác định khi gửi email: %s", e.getMessage());
            log.error("Lỗi gửi email thông báo nạp tiền thành công đến {}: {}", toEmail, errorMessage);
            throw new RuntimeException(errorMessage, e);
        }
    }

}