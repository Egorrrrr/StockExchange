package exchange;

import exchange.beans.Order;

import java.util.Comparator;

public class QtyTimeSorter implements Comparator<Order>{

    @Override
    public int compare(Order o1, Order o2) {
        if(o2.getQty().compareTo(o1.getQty()) != 0)
            return o2.getQty().compareTo(o1.getQty());
        else{
            return o2.getTime().compareTo(o1.getTime());
        }
    }

}
