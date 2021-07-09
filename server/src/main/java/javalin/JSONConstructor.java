package javalin;

import exchange.beans.Instrument;
import exchange.beans.Order;
import exchange.beans.Trade;
import exchange.beans.Trader;
import org.json.JSONObject;

import java.util.HashMap;

public class JSONConstructor {

    public static JSONObject makeJSONMarketSnapshot(HashMap<String, Instrument> instrumentMap){

        JSONObject entireSnapshot = new JSONObject();
        JSONObject instruments = new JSONObject();

        for (Instrument instrument : instrumentMap.values()
        ){

            JSONObject orders = new JSONObject();

            JSONObject sellOrders = new JSONObject();
            for (Order orderSell: instrument.getBookSell().values()
            ) {
                JSONObject orderData = new JSONObject();
                orderData.put("id", orderSell.getId());
                orderData.put("side", orderSell.getSide());
                orderData.put("price", orderSell.getPrice());
                orderData.put("instrument", orderSell.getInstrument().getName());
                orderData.put("qty", orderSell.getQty());
                sellOrders.put(orderSell.getId().toString(), orderData);
            }

            JSONObject buyOrders = new JSONObject();
            for (Order orderBuy: instrument.getBookBuy().values()
            ) {
                JSONObject orderData = new JSONObject();
                orderData.put("id", orderBuy.getId());
                orderData.put("side", orderBuy.getSide());
                orderData.put("price", orderBuy.getPrice());
                orderData.put("instrument", orderBuy.getInstrument().getName());
                orderData.put("qty", orderBuy.getQty());
                buyOrders.put(orderBuy.getId().toString(), orderData);
            }
            orders.put("buyOrders", buyOrders);
            orders.put("sellOrders", sellOrders);
            instruments.put(instrument.getName(), orders);

        }

        entireSnapshot.put("instruments",instruments);
        return entireSnapshot;
    }

    public static JSONObject getOnesOrders(Trader trader){
        JSONObject traderOrders = new JSONObject();
        for (Order order: trader.getAssociatedOrders()
        ) {
            JSONObject orderData = new JSONObject();
            orderData.put("id", order.getId());
            orderData.put("side", order.getSide());
            orderData.put("price", order.getPrice());
            orderData.put("instrument", order.getInstrument().getName());
            orderData.put("qty", order.getQty());
            traderOrders.put(order.getId().toString(), orderData);
        }
        return traderOrders;

    }
    public static JSONObject constructTrades(Trader trader){
        JSONObject tradesData = new JSONObject();

        for (Trade trade: trader.getAssociatedTrades()
        ) {
            JSONObject tradeData = new JSONObject();
            tradeData.put("price", trade.getOrder().getPrice());
            tradeData.put("qty", trade.getQty());
            tradeData.put("side", trade.getOrder().getSide());
            tradeData.put("orderId", trade.getOrder().getId());
            tradeData.put("instrument", trade.getOrder().getInstrument().getName());
            tradesData.put(trade.getId().toString(), tradeData);
        }
        return  tradesData;
    }

}
