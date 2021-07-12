package javalin;

import com.fasterxml.jackson.core.JsonProcessingException;
import exchange.MatchingEngine;
import exchange.beans.Trader;
import io.javalin.http.sse.SseClient;
import org.eclipse.jetty.server.RequestLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Notifier implements Runnable {

    private MatchingEngine matchingEngine;

    private HashMap<Trader, ArrayList<SseClient>> clientMap;
    @Override
    public void run() {
        while (true){
            try {
                sendTradeAndOrders(matchingEngine.getTradersToNotifyList());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendTradeAndOrders(Collection<Trader> tradersToNotify) throws JsonProcessingException {
        for (Trader trader: tradersToNotify
        ) {
            for (SseClient sse: clientMap.get(trader)
            ) {
                sse.sendEvent("receiveTrades" , JSONConstructor.constructTrades(trader).toString());
                sse.sendEvent("receiveOrders" , JSONConstructor.getOnesOrders(trader).toString());
            }
        }
        tradersToNotify = new ArrayList<>();
    }

    public Notifier(HashMap<Trader, ArrayList<SseClient>> clientMap, MatchingEngine matchingEngine){

        this.clientMap = clientMap;
        this.matchingEngine = matchingEngine;

    }
}
