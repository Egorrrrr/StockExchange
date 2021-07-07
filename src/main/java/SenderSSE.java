import org.json.JSONObject;

import java.util.Collection;

public class SenderSSE {

    public static void SendMarketSnapshot(JSONObject marketSnapshot, Collection<Trader> traderList){

        for (Trader trader: traderList
             ) {
            trader.sendSseMarketSnapshot(marketSnapshot);
        }

    }
}
