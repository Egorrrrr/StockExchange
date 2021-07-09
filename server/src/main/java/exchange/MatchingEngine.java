package exchange;

import exchange.beans.Instrument;
import exchange.beans.Order;
import exchange.beans.Trade;
import exchange.beans.Trader;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MatchingEngine implements Runnable{

    public BlockingQueue<Order> orderBlockingQueue;

    public HashMap<String, Instrument> instrumentMap;
    private ArrayList<Trader> tradersToNotifyList;
    HashMap<String, Trader> traderHashMap;
    HashMap<String, Trader> traderBySseCodeMap;

    public static Integer OFFERID = 0;
    public static Integer TRADEID = 0;
    public MatchingEngine(HashMap<String, Instrument> instrumentMap, HashMap<String, Trader> traderMap, HashMap<String, Trader> traderBySseCodeMap  ){

        this.instrumentMap = instrumentMap;
        this.traderHashMap = traderMap;
        this.traderBySseCodeMap = traderBySseCodeMap;
        orderBlockingQueue = new ArrayBlockingQueue<>(1);
        tradersToNotifyList = new ArrayList<>();

    }

    public ArrayList<Trader> getTradersToNotifyList() {
        return tradersToNotifyList;
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

    public ArrayList<Trader> processNewOrder(Order order) throws JsonProcessingException {

    Trader temp = order.getTrader();
    OFFERID++;
    order.setId(OFFERID);
    temp.getAssociatedOrders().add(order);
    SideEnum side = order.getSide();
    Instrument instrument = order.getInstrument();
    ArrayList<Trader> tradersToNotify = null;
    if(side.equals(SideEnum.BUY)){
        instrument.getBookBuy().put(OFFERID.toString(),order);
        orderBlockingQueue.add(order);
    }
    if(side.equals(SideEnum.SELL)){
        instrument.getBookSell().put(OFFERID.toString(),order);
        orderBlockingQueue.add(order);

    }

    return tradersToNotify;


    }

    public boolean cancelOrder(Order orderToFind){

        boolean result = true;
        Integer id  = orderToFind.getId();
        SideEnum side= orderToFind.getSide();
        Instrument instrument = orderToFind.getInstrument();
        Trader trader = orderToFind.getTrader();
        Order orderToCancel;

        if(side == SideEnum.BUY){
            orderToCancel = instrument.getBookBuy().get(id);
            if(!orderToCancel.getTrader().equals(trader)){
                result = false;
            }
            instrument.getBookBuy().remove(id);

        }
        else {
            orderToCancel = instrument.getBookSell().get(id);
            if(!orderToCancel.getTrader().equals(trader)){
                result = false;
            }
            instrument.getBookSell().remove(id);
        }
        if(result)
            trader.getAssociatedOrders().remove(orderToCancel);

        return result;


    }









    public ArrayList<Trader> findMatch(Order order) throws JsonProcessingException {

        tradersToNotifyList.add(order.getTrader());
        ArrayList<Order> matchingOrders =new ArrayList<>();
        BigDecimal priceToLookFor = order.getPrice();
        BigDecimal qty = order.getQty();
        SideEnum side = order.getSide();
        Instrument instrument = order.getInstrument();
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
                tradersToNotifyList.add(matchOrder.getTrader());
                qty = qty.subtract(matchOrder.getQty());
                yourBook.remove(matchOrder.getId().toString());
                matchOrder.getTrader().getAssociatedOrders().remove(matchOrder);
                Trade trade = new Trade(matchOrder, matchOrder.getQty(), ++TRADEID);
                matchOrder.getTrader().getAssociatedTrades().add(trade);

                Trade tradeSender = new Trade(order, matchOrder.getQty(), ++TRADEID);
                order.setQty(qty);
                order.getTrader().getAssociatedTrades().add(tradeSender);

                if(qty.compareTo(BigDecimal.valueOf(0)) == 0 ){
                    endMatching(instrument, order);

                    break;
                }

            }
            else{
                tradersToNotifyList.add(matchOrder.getTrader());
                matchOrder.setQty(matchOrder.getQty().subtract(qty));
                Trade trade = new Trade(matchOrder, qty, ++TRADEID);
                Trade tradeForSender = new Trade(order, qty, ++TRADEID);
                matchOrder.getTrader().getAssociatedTrades().add(trade);
                order.getTrader().getAssociatedTrades().add(tradeForSender);
                othersBook.remove(order.getId().toString());
                order.getTrader().getAssociatedOrders().remove(order);


                break;
            }

            i++;

        }
        return tradersToNotifyList;


    }
    public void endMatching(Instrument instrument, Order order){

        if(order.getSide() == SideEnum.SELL)
            instrument.getBookSell().remove(order.getId().toString());
        else
            instrument.getBookBuy().remove(order.getId().toString());

        order.getTrader().getAssociatedOrders().remove(order);
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


    @Override
    public void run() {
        while(true){
            try {
                findMatch(this.orderBlockingQueue.take());
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
