import org.json.JSONObject;

import javax.management.remote.JMXServerErrorException;
import java.util.ArrayList;

public class Trader {

    public String name;
    public ArrayList<Client> associatedClients;
    public ArrayList<Order> associatedOrders;

    public Trader(String name){
        associatedClients = new ArrayList<>();
        associatedOrders= new ArrayList<>();
        this.name = name;
    }

    public void sendSseMarketSnapshot(JSONObject snapshot){
        for (Client cl: associatedClients
        ) {
            System.out.println("here?");
            cl.sseClient.sendEvent("marketSnapshot", snapshot.toString());
        }
    }

    public void sendOrderEvent(String event, JSONObject data){

        for (Client cl: associatedClients
             ) {
            cl.sseClient.sendEvent(event, data.toString());
        }

    }

}
