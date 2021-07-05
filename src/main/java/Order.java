import io.javalin.http.Context;

public class Order {

    public Instrument instrument;
    public SideEnum sideEnum;
    public double price;
    private int qty;
    public Context client;

    public Order(Instrument instrument, SideEnum sideEnum, double price, int qty){
        this.instrument = instrument;
        this.sideEnum = sideEnum;
        this.price = price;
        this.qty = qty;

    }


}
