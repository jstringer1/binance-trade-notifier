package uk.co.stringerj.crypto.binance.notifier;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType;

@Service
public class Binance implements Closeable, BinanceApiCallback<UserDataUpdateEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Binance.class);

  private final BinanceApiClientFactory factory;
  private final ScheduledExecutorService executor;
  private BinanceApiRestClient rest;
  private BinanceApiWebSocketClient websocket;
  private String listenKey;
  private Closeable closeable;
  private ScheduledFuture<?> connect;

  private final List<OrderTradeUpdateListener> listeners = new CopyOnWriteArrayList<>();

  public Binance(@Value("${BINANCE_READ_KEY}") String key,
      @Value("${BINANCE_READ_SECRET}") String secret) {
    executor = Executors.newSingleThreadScheduledExecutor((r) -> {
      Thread t = new Thread(r, "Binance");
      t.setDaemon(true);
      return t;
    });
    factory = BinanceApiClientFactory.newInstance(key, secret, false, false);
    open();
  }

  public void registerListener(OrderTradeUpdateListener listener) {
    listeners.add(listener);
  }

  public void deregisterListener(OrderTradeUpdateListener listener) {
    listeners.remove(listener);
  }

  @Override
  public void onResponse(UserDataUpdateEvent event) {
    LOGGER.info(event.toString());
    if (event.getEventType() == UserDataUpdateEventType.ORDER_TRADE_UPDATE)
      listeners.forEach(listener -> listener.onTradeUpdate(event.getOrderTradeUpdateEvent()));
  }

  @Override
  public void onFailure(Throwable cause) {
    LOGGER.warn("Binance websocket error", cause);
    open();
  }

  public interface OrderTradeUpdateListener {
    void onTradeUpdate(OrderTradeUpdateEvent event);
  }

  @Override
  public void close() throws IOException {
    if (connect != null && !connect.isDone())
      connect.cancel(false);
    if (closeable == null)
      return;
    try {
      closeable.close();
    } catch (Exception ex) {
      LOGGER.warn("Error closing websocket", ex);
    } finally {
      closeable = null;
      try {
        rest.closeUserDataStream(listenKey);
      } catch (Exception ex) {
        LOGGER.warn("Error closing user data stream", ex);
      }
    }
  }

  private void open() {
    try {
      close();
      rest = factory.newRestClient();
      websocket = factory.newWebSocketClient();
      listenKey = rest.startUserDataStream();
      closeable = websocket.onUserDataUpdateEvent(listenKey, this);
      connect = executor.schedule(() -> open(), 1, TimeUnit.HOURS);
      LOGGER.info("Binance websocket open");
    } catch (Exception ex) {
      connect = executor.schedule(() -> open(), 30, TimeUnit.SECONDS);
      LOGGER.warn("Failed to open binance websocket", ex);
    }
  }
}
