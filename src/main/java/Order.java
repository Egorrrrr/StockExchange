import io.javalin.http.Context;

public class Order {

    public Instrument instrument;
    public SideEnum sideEnum;
    public double price;
    public int qty;
    public Context client;
    public Integer id;
    public Order(Instrument instrument, SideEnum sideEnum, double price, int qty, int id){
        this.instrument = instrument;
        this.sideEnum = sideEnum;
        this.price = price;
        this.qty = qty;
        this.id = id;

    }


}
