package ExchangeComponents.Beans;

import ExchangeComponents.SideEnum;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {

    private Instrument instrument;
    public Instrument getInstrument(){
        return  this.instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    private SideEnum sideEnum;
    public SideEnum getSide(){
        return this.sideEnum;
    }

    @JsonSetter("side")
    public void setSideEnum(String sideEnum) {
        this.sideEnum = SideEnum.valueOf(sideEnum);
    }

    private BigDecimal price;
    public BigDecimal getPrice(){
        return this.price;
    }
    @JsonSetter("price")
    public boolean setPrice(BigDecimal value){
        if(value.compareTo(BigDecimal.valueOf(0)) > 0) {
            this.price = value;
            return true;
        }
        return false;
    }


    private BigDecimal qty;
    public BigDecimal getQty(){
        return this.qty;
    }

    @JsonSetter("qty")
    public boolean setQty(BigDecimal value){
        if(value.compareTo(BigDecimal.valueOf(0)) > 0) {
            this.qty = value;
            return true;
        }
        return false;
    }

    private Trader trader;
    public Trader getTrader() {
        return this.trader;
    }

    public void setTrader(Trader trader) {
        this.trader = trader;
    }

    private Integer id;
    public Integer getId(){
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime Time;




}
