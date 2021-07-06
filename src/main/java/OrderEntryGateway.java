import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.sse.SseClient;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class OrderEntryGateway {

    private Javalin JAVALIN;
    private int PORT;

    public OrderEntryGateway(int port){

        PORT = port;

    }



    public void start(){

        JAVALIN = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(PORT);

        HashMap<String, Client> clientAwaitingVerification = new HashMap<>();

        HashMap<String, Client> clientMap = new HashMap<>();
        ArrayList<String> loginQueue = new ArrayList<>();

        HashMap<String, Instrument> instrumentMap = new HashMap<>();

        Instrument potato = new Instrument("Картошка");
        instrumentMap.put(potato.name, potato);
        Instrument carrot = new Instrument("Морковь");
        instrumentMap.put(carrot.name, carrot);

        MatchingEngine matchingEngine = new MatchingEngine(instrumentMap);

        JAVALIN.post("/new-order-single", matchingEngine::processNewOrder);
        JAVALIN.post("/order-cancel-request", matchingEngine::cancelOrder);


        JAVALIN.post("/login",client ->{

            String code = getRandomNumber(0, 15000).toString();
            JSONObject loginData = new JSONObject(client.body());
            String id = loginData.getString("username");
            loginQueue.add(id);


        });

        JAVALIN.post("/verify", client ->{
            JSONObject clientVerificationData = new JSONObject(client.body());
            String id  = clientVerificationData.getString("name");
            String code = clientVerificationData.getString("code");
            if(loginQueue.contains(id)){
                System.out.println("verified");
                Client verifiedClient = clientAwaitingVerification.get(code);
                clientMap.put(code, verifiedClient);
            }
            
        });

        JAVALIN.sse("/sse",client ->{
            System.out.println("yes");
            String code = getRandomNumber(0, 15000).toString();
            Client unverifiedClient = new Client(client, code);
            clientAwaitingVerification.put(code, unverifiedClient);
            client.sendEvent("connected",code);


        });


    }
    private static Integer getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);

    }

}
