package Javalin;

import ExchangeComponents.Beans.Instrument;
import ExchangeComponents.Beans.Order;
import ExchangeComponents.Beans.Trade;
import ExchangeComponents.Beans.Trader;
import ExchangeComponents.MatchingEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;


public class JSONExchange {

    private MatchingEngine engine;
    private HashMap<String, Trader> traderBySseCodeMap;
    private HashMap<String, Instrument> instrumentHashMap;
    private HashMap<String, Trader> traderHashMap;
    private HashMap<Trader, ArrayList<SseClient>> traderSSEClientMap;

    private ObjectMapper objectMapper;

    public JSONExchange(MatchingEngine matchingEngine,
                        HashMap<String, Trader> traderBySseCodeMap,
                        HashMap<String, Instrument> map,
                        HashMap<String, Trader> traderHashMap,
                        HashMap<Trader, ArrayList<SseClient>> traderSSEClientMap
    ){
        this.engine = matchingEngine;
        this.traderBySseCodeMap = traderBySseCodeMap;
        this.instrumentHashMap = map;
        this.traderHashMap = traderHashMap;
        this.traderSSEClientMap = traderSSEClientMap;
        objectMapper= new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public void createOrderFromJSON(@NotNull Context ctx) throws JsonProcessingException {


        Order order = objectMapper.readerFor(Order.class).readValue(ctx.body());
        order.setTrader(traderBySseCodeMap.get(ctx.queryParam("code")));
        order.setInstrument(instrumentHashMap.get(order.getInstrument().getName()));

        ArrayList<Trader> tradersToNotify = engine.processNewOrder(order);

        order.getTrader().sendOrderEvent("receiveOrders" , JSONConstructor.getOnesOrders(order.getTrader()));
        SenderSSE.SendMarketSnapshot(JSONConstructor.makeJSONMarketSnapshot(instrumentHashMap), traderHashMap.values());
        sendTradeAndOrders(tradersToNotify);

    }

    private void sendTradeAndOrders(ArrayList<Trader> tradersToNotify) throws JsonProcessingException {
        for (Trader trader: tradersToNotify
        ) {
            for (SseClient sse: traderSSEClientMap.get(trader)
            ) {
                sse.sendEvent("receiveTrades" , JSONConstructor.constructTrades(trader).toString());
                sse.sendEvent("receiveOrders" , JSONConstructor.getOnesOrders(trader).toString());
            }
        }
    }

    public void cancelOrderFromJSON(@NotNull Context ctx) throws JsonProcessingException {
       Trader tempTrader = traderBySseCodeMap.get(ctx.queryParam("code"));
       Order tempOrder = objectMapper.readerFor(Order.class).readValue(ctx.body());
       tempOrder.setInstrument(instrumentHashMap.get(tempOrder.getInstrument().getName()));
       boolean result = engine.cancelOrder(tempOrder);
       if(result){
           SenderSSE.SendMarketSnapshot(JSONConstructor.makeJSONMarketSnapshot(instrumentHashMap), traderHashMap.values());
           tempTrader.sendOrderEvent("receiveOrders" , JSONConstructor.getOnesOrders(tempTrader));
       }
       else{

       }

    }



}
