package ExchangeComponents;

import ExchangeComponents.Beans.Order;

import java.util.Comparator;

public class QtySorter  implements Comparator<Order>{

    @Override
    public int compare(Order o1, Order o2) {
        Integer f = 25;

        return o2.getQty().compareTo(o1.getQty());
    }

}
