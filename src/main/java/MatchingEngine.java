import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MatchingEngine {

    public HashMap<String, Instrument> instrumentMap;

    HashMap<String, Trader> traderHashMap;
    HashMap<String, Trader> traderBySseCodeMap;

    public static Integer OFFERID = 0;
    public static Integer TRADEID = 0;
    public MatchingEngine(HashMap<String, Instrument> instrumentMap,HashMap<String, Trader> traderMap, HashMap<String, Trader> traderBySseCodeMap  ){

        this.instrumentMap = instrumentMap;
        this.traderHashMap = traderMap;
        this.traderBySseCodeMap = traderBySseCodeMap;
    }

    private static Integer tryParseInt(String value, Context ctx) {
        try {

            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            throw e;
        }
    }
    private static Double tryParseDouble(String value, Context ctx) {
        try {

            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            throw e;
        }
    }

    public void processNewOrder(@NotNull Context ctx){

        JSONObject jsonOrder = new JSONObject(ctx.body());
        String code = jsonOrder.getString("code");

        Instrument instrument = instrumentMap.get(jsonOrder.getString("instrument"));

        SideEnum side =  jsonOrder.getEnum(SideEnum.class, "side");

        String qtyString = jsonOrder.getString("qty");
        String priceString = jsonOrder.getString("price");

        int qty = tryParseInt(qtyString, ctx);
        double price = tryParseDouble(priceString, ctx);

        if(qty > 0 && price > 0 && instrument != null ) {
            Trader temp = traderBySseCodeMap.get(code);
            OFFERID++;
            Order order = new Order(instrument, side, price, qty, OFFERID, temp);
            temp.associatedOrders.add(order);
            if(side.equals(SideEnum.BUY)){
                instrument.orderBookBuy.put(OFFERID.toString(),order);
                findMatchBuy(instrument, order);
            }
            if(side.equals(SideEnum.SELL)){
                instrument.orderBookSell.put(OFFERID.toString(),order);
                findMatchSell(instrument, order);

            }
            SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());
            temp.sendOrderEvent("receiveOrders" , getOnesOrders(temp));
        }
        else{
            ctx.status(HttpStatus.BAD_REQUEST_400);
            ctx.result("Wrong price or quantity");
        }

    }

    public void cancelOrder(@NotNull Context ctx){

        JSONObject cancellationData = new JSONObject(ctx.body());
        String code = cancellationData.getString("code");
        Trader trader = traderBySseCodeMap.get(code);
        SideEnum side = cancellationData.getEnum(SideEnum.class, "side");
        String id = cancellationData.getString("id");

        Instrument instrument = instrumentMap.get(cancellationData.getString("instrument"));
        Order orderToCancel;
        if(side == SideEnum.BUY){
            orderToCancel = instrument.orderBookBuy.get(id);
            if(!orderToCancel.trader.equals(trader)){
                ctx.status(HttpStatus.BAD_REQUEST_400);
                return;
            }
            instrument.orderBookBuy.remove(id);

        }
        else {

            orderToCancel = instrument.orderBookSell.get(id);
            if(!orderToCancel.trader.equals(trader)){
                ctx.status(HttpStatus.BAD_REQUEST_400);
                return;
            }
            instrument.orderBookSell.remove(id);
        }
        trader.associatedOrders.remove(orderToCancel);
        orderToCancel = null;
        SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());
        trader.sendOrderEvent("receiveOrders" , getOnesOrders(trader));

    }

    public JSONObject makeSnapshot(){

        JSONObject entireSnapshot = new JSONObject();
        JSONObject instruments = new JSONObject();

        for (Instrument instrument : instrumentMap.values()
             ){

            JSONObject orders = new JSONObject();

            JSONObject sellOrders = new JSONObject();
            for (Order orderSell: instrument.orderBookSell.values()
                 ) {
                JSONObject orderData = new JSONObject();
                orderData.put("id", orderSell.id);
                orderData.put("side", orderSell.sideEnum);
                orderData.put("price", orderSell.price);
                orderData.put("instrument", orderSell.instrument.name);
                orderData.put("qty", orderSell.qty);
                sellOrders.put(orderSell.id.toString(), orderData);
            }

            JSONObject buyOrders = new JSONObject();
            for (Order orderBuy: instrument.orderBookBuy.values()
                 ) {
                JSONObject orderData = new JSONObject();
                orderData.put("id", orderBuy.id);
                orderData.put("side", orderBuy.sideEnum);
                orderData.put("price", orderBuy.price);
                orderData.put("instrument", orderBuy.instrument.name);
                orderData.put("qty", orderBuy.qty);
                buyOrders.put(orderBuy.id.toString(), orderData);
            }
            orders.put("buyOrders", buyOrders);
            orders.put("sellOrders", sellOrders);
            instruments.put(instrument.name, orders);

        }
        entireSnapshot.put("instruments",instruments);

        return entireSnapshot;
    }

    public JSONObject getOnesOrders(Trader trader){
        JSONObject traderOrders = new JSONObject();
        for (Order order: trader.associatedOrders
             ) {
            JSONObject orderData = new JSONObject();
            orderData.put("id", order.id);
            orderData.put("side", order.sideEnum);
            orderData.put("price", order.price);
            orderData.put("instrument", order.instrument.name);
            orderData.put("qty", order.qty);
            traderOrders.put(order.id.toString(), orderData);
        }
        return traderOrders;

    }

    public void findMatchSell(Instrument instrument, Order order){

        ArrayList<Order> matchingOrders =new ArrayList<>();
        double priceToLookFor = order.price;
        Integer qty = order.qty;
        Integer totalQty = 0;
        for (Order orderToCheck: instrument.orderBookBuy.values()
             ) {
            if(orderToCheck.price == priceToLookFor){
                matchingOrders.add(orderToCheck);
                totalQty += orderToCheck.qty;
            }
        }
        matchingOrders.sort(new QtySorter());
        Collections.reverse(matchingOrders);
        int i = 0;
        int length = matchingOrders.toArray().length;
            for (Order matchOrder: matchingOrders
                 ) {
                if(qty- matchOrder.qty >= 0) {
                    qty -= matchOrder.qty;
                    instrument.orderBookBuy.remove(matchOrder.id.toString());
                    matchOrder.trader.associatedOrders.remove(matchOrder);
                    Trade trade = new Trade(matchOrder, matchOrder.qty, ++TRADEID);
                    matchOrder.trader.associatedTrades.add(trade);
                    matchOrder.trader.sendOrderEvent("receiveTrades" , constructTrades(matchOrder.trader));
                    matchOrder.trader.sendOrderEvent("receiveOrders" , getOnesOrders(matchOrder.trader));
                    //создаем трейд
                    if(qty == 0){
                        endMatching(instrument, order);
                        order.trader.sendOrderEvent("receiveTrades" , constructTrades(order.trader));
                        order.trader.sendOrderEvent("receiveOrders" , getOnesOrders(order.trader));
                        break;
                    }
                    if(i == length-1){
                        Trade tradeSender = new Trade(order, order.qty - qty, ++TRADEID);
                        order.qty = qty;
                        order.trader.associatedTrades.add(tradeSender);
                        sendTradeAndOrders(order);
                        matchOrder.trader.associatedTrades.add(trade);
                    }
                    //удалить полностью
                }
                else{
                    matchOrder.qty -= qty;

                    Trade trade = new Trade(matchOrder, qty, ++TRADEID);
                    matchOrder.trader.associatedTrades.add(trade);
                    Trade tradeForSender = new Trade(order, order.qty, ++TRADEID);
                    order.trader.associatedTrades.add(tradeForSender);
                    instrument.orderBookSell.remove(order.id.toString());
                    order.trader.associatedOrders.remove(order);
                    SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());

                    matchOrder.trader.sendOrderEvent("receiveTrades" , constructTrades(matchOrder.trader));
                    order.trader.sendOrderEvent("receiveTrades" , constructTrades(order.trader));
                    matchOrder.trader.sendOrderEvent("receiveOrders" , getOnesOrders(matchOrder.trader));
                    order.trader.sendOrderEvent("receiveOrders" , getOnesOrders(order.trader));
                    System.out.println(3);
                    //создаем трейд
                    break;
                }


            }



    }



    public void findMatchBuy(Instrument instrument, Order order){

        ArrayList<Order> matchingOrders =new ArrayList<>();
        double priceToLookFor = order.price;
        Integer qty = order.qty;
        Integer totalQty = 0;
        for (Order orderToCheck: instrument.orderBookSell.values()
        ) {
            if(orderToCheck.price == priceToLookFor){
                matchingOrders.add(orderToCheck);
                totalQty += orderToCheck.qty;
            }
        }
        matchingOrders.sort(new QtySorter());
        Collections.reverse(matchingOrders);
        int i = 0;
        int length = matchingOrders.toArray().length;
        for (Order matchOrder: matchingOrders
        ) {
            if(qty- matchOrder.qty >= 0) {
                qty -= matchOrder.qty;
                instrument.orderBookSell.remove(matchOrder.id.toString());
                matchOrder.trader.associatedOrders.remove(matchOrder);
                Trade trade = new Trade(matchOrder, matchOrder.qty, ++TRADEID);
                matchOrder.trader.associatedTrades.add(trade);
                sendTradeAndOrders(matchOrder);

                if(qty == 0){
                    endMatching(instrument, order);
                    sendTradeAndOrders(order);
                }
                if(i == length-1){
                    Trade tradeSender = new Trade(order, order.qty - qty, ++TRADEID);
                    order.qty = qty;
                    order.trader.associatedTrades.add(tradeSender);
                    sendTradeAndOrders(order);
                    matchOrder.trader.associatedTrades.add(trade);
                }
            }
            else{
                matchOrder.qty -= qty;

                Trade trade = new Trade(matchOrder, qty, ++TRADEID);
                matchOrder.trader.associatedTrades.add(trade);
                Trade tradeForSender = new Trade(order, order.qty, ++TRADEID);
                order.trader.associatedTrades.add(tradeForSender);
                instrument.orderBookBuy.remove(order.id.toString());
                order.trader.associatedOrders.remove(order);
                SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());

                sendTradeAndOrders(order);
                sendTradeAndOrders(matchOrder);
                break;
            }

            i++;

        }



    }
    public void endMatching(Instrument instrument, Order order){
        if(order.sideEnum == SideEnum.SELL)
            instrument.orderBookSell.remove(order.id.toString());
        else
            instrument.orderBookBuy.remove(order.id.toString());



        order.trader.associatedOrders.remove(order);
        Trade tradeForSender = new Trade(order, order.qty, ++TRADEID);
        order.trader.associatedTrades.add(tradeForSender);
        SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());
    }

    private void sendTradeAndOrders(Order order){
        order.trader.sendOrderEvent("receiveTrades" , constructTrades(order.trader));
        order.trader.sendOrderEvent("receiveOrders" , getOnesOrders(order.trader));
    }


    public JSONObject constructTrades(Trader trader){
        JSONObject tradesData = new JSONObject();

        for (Trade trade: trader.associatedTrades
             ) {
            JSONObject tradeData = new JSONObject();
            tradeData.put("total", trade.totalPrice);
            tradeData.put("price", trade.order.price);
            tradeData.put("qty", trade.qty);
            tradeData.put("side", trade.order.sideEnum);
            tradeData.put("orderId", trade.order.id);
            tradeData.put("instrument", trade.order.instrument.name);
            tradesData.put(trade.id.toString(), tradeData);
        }
        return  tradesData;
    }

}

