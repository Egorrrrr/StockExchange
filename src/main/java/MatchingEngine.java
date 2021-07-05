import io.javalin.http.Context;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.json.HTTP;
import org.json.JSONObject;

import java.util.HashMap;

public class MatchingEngine {

    public HashMap<String, Instrument> instrumentMap;

    public MatchingEngine(HashMap<String, Instrument> instrumentMap){

        this.instrumentMap = instrumentMap;

    }

    private static Integer tryParseInt(String value, Context ctx) {
        try {

            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            throw e;
        }
    }
    private static Double tryParseDouble(String value, Context ctx) {
        try {

            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST_400);
            throw e;
        }
    }

    public void processNewOrder(@NotNull Context ctx){

        JSONObject jsonOrder = new JSONObject(ctx.body());


        Instrument instrument = instrumentMap.get(jsonOrder.getString("instrument"));

        SideEnum side =  jsonOrder.getEnum(SideEnum.class, "side");

        String qtyString = jsonOrder.getString("qty");
        String priceString = jsonOrder.getString("price");

        int qty = tryParseInt(qtyString, ctx);
        double price = tryParseDouble(priceString, ctx);

        if(qty > 0 && price > 0 && instrument != null ) {
            Order order = new Order(instrument, side, price, qty);
            if(side.equals(SideEnum.BUY)){
                instrument.orderBookBuy.add(order);
            }
            if(side.equals(SideEnum.SELL)){

                instrument.orderBookSell.add(order);

            }
        }
        else{
            ctx.result("Wrong price or quantity");
        }

    }

    public void cancelOrder(@NotNull Context ctx){



    }


}
