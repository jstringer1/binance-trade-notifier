package uk.co.stringerj.crypto.binance.notifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.threeten.bp.Instant;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;

@Service
public class Notifer {

  private static final Logger LOGGER = LoggerFactory.getLogger(Notifer.class);

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
      message.setText(
          String.format(
              "ORDER_ID: %d ( %s )\n" + "SIDE: %s\n" + "CREATED: %s\n" + "UPDATED: %s\n"
                  + "QUANTITY: %s\n" + "QUOTE QUANTITY: %s\n" + "LIMIT PRICE: %s\n" + "PRICE: %f%s",
              event.getOrderId(), event.getNewClientOrderId(), event.getSide(),
              Instant.ofEpochMilli(event.getOrderCreationTime()),
              Instant.ofEpochMilli(event.getEventTime()), event.getAccumulatedQuantity(),
              event.getCumulativeQuoteQty(), event.getPrice(), getPrice(event), getPrices(event)),
          false);
    };
    try {
      mailer.send(preparator);
    } catch (Exception ex) {
      LOGGER.warn("FAILED TO SEND EMAIL", ex);
    }
  }

  private String getPrices(OrderTradeUpdateEvent event) {
    if (event.getOrderStatus() != OrderStatus.FILLED)
      return "";
    return getPrices(getPrice(event), event.getSide() == OrderSide.BUY);
  }

  private String getPrices(double price, boolean add) {
    StringBuilder sb = new StringBuilder().append("\n\n");
    sb.append(String.format("00.2%% = %f\n", add ? price * 1.002 : price * 0.998));
    sb.append(String.format("00.5%% = %f\n", add ? price * 1.005 : price * 0.995));
    sb.append(String.format("01.0%% = %f\n", add ? price * 1.01 : price * 0.99));
    sb.append(String.format("02.0%% = %f\n", add ? price * 1.02 : price * 0.98));
    sb.append(String.format("03.0%% = %f\n", add ? price * 1.03 : price * 0.97));
    sb.append(String.format("04.0%% = %f\n", add ? price * 1.04 : price * 0.96));
    sb.append(String.format("05.0%% = %f\n", add ? price * 1.05 : price * 0.95));
    sb.append(String.format("10.0%% = %f\n", add ? price * 1.1 : price * 0.9));
    return sb.toString();
  }

  private double getPrice(OrderTradeUpdateEvent event) {
    double quantity = Double.parseDouble(event.getAccumulatedQuantity());
    double quoteQuantity = Double.parseDouble(event.getCumulativeQuoteQty());
    if (quantity <= 0 || quoteQuantity <= 0)
      return 0;
    return (quoteQuantity / quantity);
  }
}
