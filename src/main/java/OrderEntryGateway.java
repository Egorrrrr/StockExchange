import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.sse.SseClient;

import java.util.HashMap;

public class OrderEntryGateway {

    private Javalin JAVALIN;
    private int PORT;

    public OrderEntryGateway(int port){

        PORT = port;

    }



    public void start(){

        JAVALIN = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(PORT);
        HashMap<String, SseClient> clientMap = new HashMap<>();
        HashMap<String, Instrument> instrumentMap = new HashMap<>();

        Instrument potato = new Instrument("Картошка");
        instrumentMap.put(potato.name, potato);
        Instrument carrot = new Instrument("Морковь");
        instrumentMap.put(carrot.name, carrot);

        MatchingEngine matchingEngine = new MatchingEngine(instrumentMap);

        JAVALIN.post("/new-order-single", matchingEngine::processNewOrder);
        JAVALIN.post("/order-cancel-request", matchingEngine::cancelOrder);


        JAVALIN.post("/login",client ->{



        });

        JAVALIN.sse("/sse",client ->{

            String code = getRandomNumber(0, 15000).toString();
            clientMap.put(code, client);
            client.sendEvent("connected",code);

        });


    }
    private static Integer getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);

    }

}
