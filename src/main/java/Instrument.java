import java.util.ArrayList;
import java.util.HashMap;

public class Instrument {

    public String name;
    public HashMap<String ,Order> orderBookSell;
    public HashMap<String, Order> orderBookBuy;

    public Instrument(String name){

        this.name = name;
        orderBookSell = new HashMap<>();
        orderBookBuy = new HashMap<>();

    }
}
