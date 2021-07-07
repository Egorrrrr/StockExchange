import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.sse.SseClient;
import org.json.JSONObject;

import java.io.*;
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


    private HashMap<String, Instrument> readFromInputStream(InputStream inputStream)
            throws IOException {
        HashMap<String, Instrument> instrumentHashMap = new HashMap<>();
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                Instrument instrument = new Instrument(line);
                instrumentHashMap.put(line, instrument);
            }
        }
        return instrumentHashMap;
    }

    public void start() throws IOException {

        HashMap<String, Client> clientAwaitingVerification = new HashMap<>();
        HashMap<String, Trader> traderBySseCode = new HashMap<>();
        HashMap<String, Trader> traderMap = new HashMap<>();
        ArrayList<String> loginQueue = new ArrayList<>();

        //запуск javalin
        JAVALIN = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(PORT);

        //считываем файл с инструментами и зансим их в словарь в классе MatchingEngine
        InputStream instrumentFile = new FileInputStream("src/main/java/ins.txt");
        MatchingEngine matchingEngine = new MatchingEngine(readFromInputStream(instrumentFile),traderMap, traderBySseCode);




        System.out.println(matchingEngine.makeSnapshot().toString());

        JAVALIN.post("/new-order-single", matchingEngine::processNewOrder);
        JAVALIN.post("/order-cancel-request", matchingEngine::cancelOrder);


        JAVALIN.post("/login",client ->{

            JSONObject loginData = new JSONObject(client.body());
            String id = loginData.getString("username");
            loginQueue.add(id);
            Trader trader = new Trader(id);
            traderMap.put(id, trader);



        });

        JAVALIN.post("/verify", client ->{
            JSONObject clientVerificationData = new JSONObject(client.body());
            String id  = clientVerificationData.getString("name");
            String code = clientVerificationData.getString("code");
            if(loginQueue.contains(id)){

                Client verifiedClient = clientAwaitingVerification.get(code);

                Trader temp = traderMap.get(id);
                temp.associatedClients.add(verifiedClient);
                traderBySseCode.put(code, temp);
                verifiedClient.sseClient.sendEvent("marketSnapshot",matchingEngine.makeSnapshot().toString());
            }

            
        });

        JAVALIN.sse("/sse",client ->{
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
