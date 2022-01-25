package uk.co.crypto.binance.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.threeten.bp.Instant;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;

@Service
public class Notifer {

  private final JavaMailSender mailer;
  private final String from;
  private final String to;

  public Notifer(@Autowired Binance binance, @Autowired JavaMailSender mailer,
      @Value("${EMAIL_NOTIFICATION_FROM}") String from,
      @Value("${EMAIL_NOTIFICATION_TO}") String to) {
    this.mailer = mailer;
    this.from = from;
    this.to = to;
    binance.registerListener(this::onTradeUpdate);
  }

  private void onTradeUpdate(OrderTradeUpdateEvent event) {
    MimeMessagePreparator preparator = mimeMessage -> {
      MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
      message.setTo(to);
      message.setFrom(from);
      message.setSubject(
          String.format("BINANCE ORDER UPDATE: %s %s", event.getSymbol(), event.getOrderStatus()));
      message.setText(String.format(
          "ORDER_ID: %d ( %s )\n" + "SIDE: %s\n" + "CREATED: %s\n" + "UPDATED: %s\n"
              + "QUANTITY: %s\n" + "QUOTE QUANTITY: %s\n" + "LIMIT PRICE: %s\n" + "PRICE: %s",
          event.getOrderId(), event.getNewClientOrderId(), event.getSide(),
          Instant.ofEpochMilli(event.getOrderCreationTime()),
          Instant.ofEpochMilli(event.getEventTime()), event.getAccumulatedQuantity(),
          event.getCumulativeQuoteQty(), event.getPrice(), getPrice(event)), false);
    };
    mailer.send(preparator);
  }

  private String getPrice(OrderTradeUpdateEvent event) {
    double quantity = Double.parseDouble(event.getAccumulatedQuantity());
    double quoteQuantity = Double.parseDouble(event.getCumulativeQuoteQty());
    if (quantity <= 0 || quoteQuantity <= 0)
      return "0";
    return String.valueOf(quoteQuantity / quantity);
  }
}
