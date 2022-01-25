package uk.co.crypto.binance.notifier;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.OrderTradeUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent;
import com.binance.api.client.domain.event.UserDataUpdateEvent.UserDataUpdateEventType;

@Service
public class Binance implements Closeable {

  private final BinanceApiClientFactory factory;
  private final BinanceApiRestClient rest;
  private final BinanceApiWebSocketClient websocket;
  private final String listenKey;
  private final Closeable closeable;

  private final List<OrderTradeUpdateListener> listeners = new CopyOnWriteArrayList<>();

  public Binance(@Value("${sm://BINANCE_READ_KEY}") String key,
      @Value("${sm://BINANCE_READ_SECRET}") String secret) {
    factory = BinanceApiClientFactory.newInstance(key, secret, false, false);
    rest = factory.newRestClient();
    websocket = factory.newWebSocketClient();
    listenKey = rest.startUserDataStream();
    closeable = websocket.onUserDataUpdateEvent(listenKey, this::onUserDataUpdate);
  }

  public void registerListener(OrderTradeUpdateListener listener) {
    listeners.add(listener);
  }

  public void deregisterListener(OrderTradeUpdateListener listener) {
    listeners.remove(listener);
  }

  private void onUserDataUpdate(UserDataUpdateEvent event) {
    if (event.getEventType() == UserDataUpdateEventType.ORDER_TRADE_UPDATE)
      listeners.forEach(listener -> listener.onTradeUpdate(event.getOrderTradeUpdateEvent()));
  }

  public interface OrderTradeUpdateListener {
    void onTradeUpdate(OrderTradeUpdateEvent event);
  }

  @Override
  public void close() throws IOException {
    closeable.close();
    rest.closeUserDataStream(listenKey);
  }
}
