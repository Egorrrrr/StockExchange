import io.javalin.http.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {

    private Instrument instrument;
    public Instrument getInstrument(){
        return  this.instrument;
    }

    private SideEnum sideEnum;
    public SideEnum getSide(){
        return this.sideEnum;
    }

    private BigDecimal price;
    public BigDecimal getPrice(){
        return this.price;
    }
    public boolean setPrice(BigDecimal value){
        this.price = value;
        return true;
    }


    private BigDecimal qty;
    public BigDecimal getQty(){
        return this.qty;
    }
    public boolean setQty(BigDecimal value){
        this.qty = value;
        return true;
    }

    private Trader trader;
    public Trader getTrader() {
        return this.trader;
    }

    private Integer id;
    public Integer getId(){
        return this.id;
    }

    public LocalDateTime Time;
    public Order(Instrument instrument, SideEnum sideEnum, BigDecimal price, BigDecimal qty, int id, Trader trader){
        this.instrument = instrument;
        this.sideEnum = sideEnum;
        this.price = price;
        this.qty = qty;
        this.id = id;
        this.trader = trader;

    }


}
