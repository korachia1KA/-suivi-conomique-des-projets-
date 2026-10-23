package com.example.project.service;

import com.example.project.config.Translator;
import com.example.project.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Service implementation for managing email notifications.
 * Provides methods for sending signup confirmation and password recovery emails.
 */

@RequiredArgsConstructor
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendSignupEmail(String to, String firstName, String confirmLink) {
        try {
            Locale locale = Translator.getCurrentLocale();

            Context thymeleafContext = new Context(locale);
            thymeleafContext.setVariable("firstName", firstName);
            thymeleafContext.setVariable("link", confirmLink);

            String htmlContent = templateEngine.process("verification", thymeleafContext);

            String textContent = Translator.toLocale("mail.verify.title") + "\n\n" +
                    Translator.toLocale("mail.verify.thankyou").replaceAll("<[^>]*>", "") + "\n" +
                    Translator.toLocale("mail.verify.instruction") + "\n" +
                    confirmLink + "\n\n" +
                    Translator.toLocale("mail.verify.footer");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = Translator.toLocale("user.signup.subject");
            helper.setFrom("no-reply@azienda.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new BusinessException("EMAIL_SENDING_FAILED");
        }
    }

    @Override
    public void sendForgotPasswordEmail(String to, String firstName, String otp) {
        try {

            Locale locale = Translator.getCurrentLocale();

            Context thymeleafContext = new Context(locale);
            thymeleafContext.setVariable("firstName", firstName);
            thymeleafContext.setVariable("otp", otp);

            String htmlContent = templateEngine.process("reset-password", thymeleafContext);

            String textContent =
                    Translator.toLocale("mail.reset.title") + "\n\n" +
                            Translator.toLocale("mail.reset.greeting").replaceAll("<[^>]*>", "") + "\n\n" +
                            Translator.toLocale("mail.reset.instruction") + "\n\n" +
                            otp + "\n\n" +
                            Translator.toLocale("mail.reset.footer");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = Translator.toLocale("user.reset.subject");

            helper.setFrom("no-reply@azienda.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new BusinessException("EMAIL_SENDING_FAILED");
        }
    }
}