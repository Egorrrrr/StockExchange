import org.json.JSONObject;

import javax.management.remote.JMXServerErrorException;
import java.util.ArrayList;

public class Trader {

    private String name;
    public String getName(){
        return this.name;
    }
    public boolean setName(String value){
        this.name = value;
        return true;
    }

    private ArrayList<Client> associatedClients;
    public ArrayList<Client> getAssociatedClients(){
        return this.associatedClients;
    }

    private ArrayList<Order> associatedOrders;
    public ArrayList<Order> getAssociatedOrders(){
        return this.associatedOrders;
    }

    private ArrayList<Trade> associatedTrades;
    public ArrayList<Trade> getAssociatedTrades(){
        return this.associatedTrades;
    }

    public Trader(String name){
        associatedClients = new ArrayList<>();
        associatedOrders= new ArrayList<>();
        associatedTrades = new ArrayList<>();
        this.name = name;
    }

    public void sendSseMarketSnapshot(JSONObject snapshot){
        for (Client cl: associatedClients
        ) {
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
