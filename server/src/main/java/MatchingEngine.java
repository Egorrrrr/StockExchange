import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.math.BigDecimal;
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

    private static BigDecimal tryParseDecimal(String value, Context ctx) {
        try {

            return  new BigDecimal(value);
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

        BigDecimal qty = tryParseDecimal(qtyString, ctx);
        BigDecimal price = tryParseDecimal(priceString, ctx);

        if(qty.compareTo(BigDecimal.valueOf(0)) > 0 && price.compareTo(BigDecimal.valueOf(0)) > 0 && instrument != null ) {
            Trader temp = traderBySseCodeMap.get(code);
            OFFERID++;
            Order order = new Order(instrument, side, price, qty, OFFERID, temp);
            temp.getAssociatedOrders().add(order);
            if(side.equals(SideEnum.BUY)){
                instrument.getBookBuy().put(OFFERID.toString(),order);
                findMatch(instrument, order, side);
            }
            if(side.equals(SideEnum.SELL)){
                instrument.getBookSell().put(OFFERID.toString(),order);
                findMatch(instrument, order, side);

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
            orderToCancel = instrument.getBookBuy().get(id);
            if(!orderToCancel.getTrader().equals(trader)){
                ctx.status(HttpStatus.BAD_REQUEST_400);
                return;
            }
            instrument.getBookBuy().remove(id);

        }
        else {

            orderToCancel = instrument.getBookSell().get(id);
            if(!orderToCancel.getTrader().equals(trader)){
                ctx.status(HttpStatus.BAD_REQUEST_400);
                return;
            }
            instrument.getBookSell().remove(id);
        }
        trader.getAssociatedOrders().remove(orderToCancel);
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

    public JSONObject getOnesOrders(Trader trader){
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





    public void findMatch(Instrument instrument, Order order, SideEnum side){

        ArrayList<Order> matchingOrders =new ArrayList<>();
        BigDecimal priceToLookFor = order.getPrice();
        BigDecimal qty = order.getQty();
        HashMap<String,Order>  yourBook = side.equals(SideEnum.BUY) ? instrument.getBookSell() : instrument.getBookBuy();
        HashMap<String,Order>  othersBook = side.equals(SideEnum.BUY) ? instrument.getBookBuy() : instrument.getBookSell();
        for (Order orderToCheck: yourBook.values()
        ) {

            if(orderToCheck.getPrice().compareTo(priceToLookFor) == 0 && !order.getTrader().equals(orderToCheck.getTrader())){
                matchingOrders.add(orderToCheck);
            }
        }
        matchingOrders.sort(new QtySorter());
        Collections.reverse(matchingOrders);
        int i = 0;
        int length = matchingOrders.toArray().length;
        for (Order matchOrder: matchingOrders
        ) {
            if(qty.subtract(matchOrder.getQty()).compareTo(BigDecimal.valueOf(0))  >= 0) {
                qty = qty.subtract(matchOrder.getQty());
                yourBook.remove(matchOrder.getId().toString());
                matchOrder.getTrader().getAssociatedOrders().remove(matchOrder);
                Trade trade = new Trade(matchOrder, matchOrder.getQty(), ++TRADEID);
                matchOrder.getTrader().getAssociatedTrades().add(trade);
                sendTradeAndOrders(matchOrder);

                if(qty.compareTo(BigDecimal.valueOf(0)) == 0 ){
                    endMatching(instrument, order);
                    sendTradeAndOrders(order);
                    break;
                }
                if(i == length-1){
                    Trade tradeSender = new Trade(order, order.getQty().subtract(qty), ++TRADEID);
                    order.setQty(qty);
                    order.getTrader().getAssociatedTrades().add(tradeSender);
                    sendTradeAndOrders(order);
                    matchOrder.getTrader().getAssociatedTrades().add(trade);
                }
            }
            else{
                matchOrder.setQty(matchOrder.getQty().subtract(qty));

                Trade trade = new Trade(matchOrder, qty, ++TRADEID);
                matchOrder.getTrader().getAssociatedTrades().add(trade);
                Trade tradeForSender = new Trade(order, order.getQty(), ++TRADEID);
                order.getTrader().getAssociatedTrades().add(tradeForSender);
                othersBook.remove(order.getId().toString());
                order.getTrader().getAssociatedOrders().remove(order);

                SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());
                sendTradeAndOrders(order);
                sendTradeAndOrders(matchOrder);
                break;
            }

            i++;

        }



    }
    public void endMatching(Instrument instrument, Order order){

        if(order.getSide() == SideEnum.SELL)
            instrument.getBookSell().remove(order.getId().toString());
        else
            instrument.getBookBuy().remove(order.getId().toString());

        order.getTrader().getAssociatedOrders().remove(order);
        Trade tradeForSender = new Trade(order, order.getQty(), ++TRADEID);
        order.getTrader().getAssociatedTrades().add(tradeForSender);
        SenderSSE.SendMarketSnapshot(makeSnapshot(), traderHashMap.values());
    }

    private void sendTradeAndOrders(Order order){
        order.getTrader().sendOrderEvent("receiveTrades" , constructTrades(order.getTrader()));
        order.getTrader().sendOrderEvent("receiveOrders" , getOnesOrders(order.getTrader()));
    }


    public JSONObject constructTrades(Trader trader){
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

