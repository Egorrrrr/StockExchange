


public class Trade {

    public Order order;
    public Integer qty;
    public Integer id;
    public double totalPrice;

    public Trade(Order order, Integer qty, Integer id){
        this.order =order;
        this.qty = qty;
        this.totalPrice = order.price * qty;
        this.id = id;
    }

}
