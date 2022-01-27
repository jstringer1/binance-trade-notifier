package uk.co.stringerj.crypto.binance.notifier;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@SpringBootApplication
public class BinanceTradeNotifierApplication {

  public static void main(String[] args) {
    SpringApplication.run(BinanceTradeNotifierApplication.class, args);
  }

  @Bean
  public JavaMailSender mailer(@Value("${EMAIL_NOTIFICATION_HOST}") String host,
      @Value("${EMAIL_NOTIFICATION_FROM}") String from,
      @Value("${EMAIL_NOTIFICATION_PASSWORD}") String password) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(host);
    mailSender.setPort(587);
    mailSender.setUsername(from);
    mailSender.setPassword(password);
    Properties props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.debug", "false");
    return mailSender;
  }

}
