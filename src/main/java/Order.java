import io.javalin.http.Context;

public class Order {

    public Instrument instrument;
    public SideEnum sideEnum;
    public double price;
    public Integer qty;
    public Trader trader;
    public Integer id;
    public Order(Instrument instrument, SideEnum sideEnum, double price, int qty, int id, Trader trader){
        this.instrument = instrument;
        this.sideEnum = sideEnum;
        this.price = price;
        this.qty = qty;
        this.id = id;
        this.trader = trader;

    }


}
