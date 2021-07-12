package exchange.beans;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Instrument {

    private String name;
    public String getName(){
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }

    private ConcurrentHashMap<String , Order> orderBookSell;
    public ConcurrentHashMap<String ,Order> getBookSell(){
        return orderBookSell;
    }

    private ConcurrentHashMap<String, Order> orderBookBuy;
    public ConcurrentHashMap<String, Order> getBookBuy(){
        return orderBookBuy;
    }

    public Instrument(String name){

        this.name = name;
        orderBookSell = new ConcurrentHashMap<>();
        orderBookBuy = new ConcurrentHashMap<>();

    }
}
