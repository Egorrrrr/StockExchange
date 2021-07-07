import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashMap;

public class MatchingEngine {

    public HashMap<String, Instrument> instrumentMap;

    HashMap<String, Trader> traderHashMap;
    HashMap<String, Trader> traderBySseCodeMap;

    public static Integer ID = 0;
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
            Order order = new Order(instrument, side, price, qty, ++ID, temp);
            temp.associatedOrders.add(order);
            if(side.equals(SideEnum.BUY)){
                instrument.orderBookBuy.put(ID.toString(),order);
            }
            if(side.equals(SideEnum.SELL)){
                instrument.orderBookSell.put(ID.toString(),order);

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
            instrument.orderBookBuy.remove(id);

        }
        else {
            orderToCancel = instrument.orderBookSell.get(id);
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

}
