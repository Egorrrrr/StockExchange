import java.util.ArrayList;

public class Instrument {

    public String name;
    public ArrayList<Order> orderBookSell;
    public ArrayList<Order> orderBookBuy;

    public Instrument(String name){

        this.name = name;
        orderBookSell = new ArrayList();
        orderBookBuy = new ArrayList();

    }
}
