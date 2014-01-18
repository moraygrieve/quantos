package com.kaia.exchange.fix.market;

import com.kaia.exchange.core.engine.Engine;
import com.kaia.exchange.core.utils.Observable;
import com.kaia.exchange.fix.utils.SettingsAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;
import quickfix.field.MDReqID;
import quickfix.field.SubscriptionRequestType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BookSubscriptionManager extends Observable<BookSubscriber> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookSubscriptionManager.class);
    private final Engine engine;
    private final BookMessageHandler handler;
    private Map<SessionID, Map<String, BookSubscriber>> subscriptions;

    public BookSubscriptionManager(Engine engine, BookMessageHandler handler) {
        this.engine = engine;
        this.handler = handler;
        subscriptions = new HashMap<SessionID, Map<String, BookSubscriber>>();
    }

    public void add(SessionID session, String symbol, SubscriptionRequestType type, MDReqID mdReqID) {
        if (!subscriptions.containsKey(session)) {
            subscriptions.put(session, new HashMap<String, BookSubscriber>());
        }

        if (!subscriptions.get(session).containsKey(mdReqID.getValue())) {
            LOGGER.info("Adding subscription[" + symbol + "]: " + SettingsAccessor.getAccount(session) + "|" + mdReqID.getValue());
            BookSubscriber sub = new BookSubscriber(handler, type, session, mdReqID, symbol);
            engine.subscribeBook(symbol, sub);
            subscriptions.get(session).put(mdReqID.getValue(), sub);
            notifyObservers(sub);
        }
    }

    public void remove(SessionID session, MDReqID mdReqID) {
        if (subscriptions.containsKey(session)) {
            if (!subscriptions.get(session).containsKey(mdReqID.getValue())) {
                LOGGER.error("Request to remove subscription for unknown mdReqID " + session + ":" + mdReqID.getValue());
                return;
            }
            BookSubscriber subscriber = subscriptions.get(session).get(mdReqID.getValue());
            unsubscribe(subscriber);
            subscriptions.get(session).remove(subscriber.getMdReqID().getValue());
            if (subscriptions.get(session).size() == 0) {
                subscriptions.remove(session);
            }
        }
    }

    public void remove(SessionID session) {
        if (subscriptions.containsKey(session)) {
            Iterator<String> it = subscriptions.get(session).keySet().iterator();
            while (it.hasNext()) {
                unsubscribe(subscriptions.get(session).get(it.next()));
                it.remove();
            }
            subscriptions.remove(session);
        }
    }

    private void unsubscribe(BookSubscriber subscriber) {
        LOGGER.info("Removing subscription[" + subscriber.getSymbol() + "]: " + subscriber.getAccount() + "|" + subscriber.getMdReqID().getValue());
        engine.unsubscribeBook(subscriber.getSymbol(), subscriber);
    }

    public boolean hasSubscriptions(SessionID session) {
        return subscriptions.containsKey(session);
    }
}