package exchange.beans;

import java.util.HashMap;

public class Instrument {

    private String name;
    public String getName(){
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }

    private HashMap<String , Order> orderBookSell;
    public HashMap<String ,Order> getBookSell(){
        return orderBookSell;
    }

    private HashMap<String, Order> orderBookBuy;
    public HashMap<String, Order> getBookBuy(){
        return orderBookBuy;
    }

    public Instrument(String name){

        this.name = name;
        orderBookSell = new HashMap<>();
        orderBookBuy = new HashMap<>();

    }
}
