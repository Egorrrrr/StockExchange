import java.util.Comparator;

public class QtySorter  implements Comparator<Order>{

    @Override
    public int compare(Order o1, Order o2) {
        return o2.qty.compareTo(o1.qty);
    }

}
