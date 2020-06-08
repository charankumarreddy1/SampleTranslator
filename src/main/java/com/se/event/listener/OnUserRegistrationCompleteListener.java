package com.se.event.listener;

import freemarker.template.TemplateException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.se.event.OnUserRegistrationCompleteEvent;
import com.se.exception.MailSendException;
import com.se.model.User;
import com.se.service.EmailVerificationTokenService;
import com.se.service.MailService;

import javax.mail.MessagingException;
import java.io.IOException;

@Component
public class OnUserRegistrationCompleteListener implements ApplicationListener<OnUserRegistrationCompleteEvent> {

    private static final Logger logger = Logger.getLogger(OnUserRegistrationCompleteListener.class);
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final MailService mailService;

    @Autowired
    public OnUserRegistrationCompleteListener(EmailVerificationTokenService emailVerificationTokenService, MailService mailService) {
        this.emailVerificationTokenService = emailVerificationTokenService;
        this.mailService = mailService;
    }

    /**
     * As soon as a registration event is complete, invoke the email verification
     * asynchronously in an another thread pool
     */
    @Override
    @Async
    public void onApplicationEvent(OnUserRegistrationCompleteEvent onUserRegistrationCompleteEvent) {
        sendEmailVerification(onUserRegistrationCompleteEvent);
    }

    /**
     * Send email verification to the user and persist the token in the database.
     */
    private void sendEmailVerification(OnUserRegistrationCompleteEvent event) {
        User user = event.getUser();
        String token = emailVerificationTokenService.generateNewToken();
        emailVerificationTokenService.createVerificationToken(user, token);

        String recipientAddress = user.getEmail();
        String emailConfirmationUrl = event.getRedirectUrl().queryParam("token", token).toUriString();

        try {
            mailService.sendEmailVerification(emailConfirmationUrl, recipientAddress);
        } catch (IOException | TemplateException | MessagingException e) {
            logger.error(e);
            throw new MailSendException(recipientAddress, "Email Verification");
        }
    }
}
