import org.json.JSONObject;

import java.util.Collection;

public class SenderSSE {

    public static void SendMarketSnapshot(JSONObject marketSnapshot, Collection<Client> v){
        for (Client cl: v
             ) {
            System.out.println("here?");
            cl.sseClient.sendEvent("marketSnapshot", marketSnapshot.toString());
        }

    }
}
