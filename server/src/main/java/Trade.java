import java.math.BigDecimal;

public class Trade {

    private Order order;
    public Order getOrder(){
        return this.order;
    }

    private BigDecimal qty;
    public BigDecimal getQty(){
        return this.qty;
    }


    private Integer id;
    public Integer getId(){
        return this.id;
    }

    public Trade(Order order, BigDecimal qty, Integer id){
        this.order =order;
        this.qty = qty;
        this.id = id;
    }

}
