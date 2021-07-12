package javalin;

import exchange.beans.Client;
import exchange.beans.Instrument;
import exchange.MatchingEngine;
import exchange.beans.Trader;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.sse.SseClient;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class OrderEntryGateway {

    private Javalin JAVALIN;
    private int PORT;

    public OrderEntryGateway(int port){

        PORT = port;

    }


    private ConcurrentHashMap<String, Instrument> readInstrumentsFromInputStream(InputStream inputStream)
            throws IOException {
        ConcurrentHashMap<String, Instrument> instrumentHashMap = new ConcurrentHashMap<>();
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

        HashMap<String, Client> clientAwaitingVerification = new HashMap<>(); //code-cleint list
        HashMap<String, Trader> traderBySseCode = new HashMap<>(); //get trader by sse
        HashMap<String, Trader> traderMap = new HashMap<>(); // get trader by name
        HashMap<Trader, ArrayList<SseClient>> traderSSEClientMap = new HashMap<>(); // clients by sse

        ArrayList<String> loginQueue = new ArrayList<>();

        //запуск javalin
        JAVALIN = Javalin.create(JavalinConfig::enableCorsForAllOrigins).start(PORT);

        //считываем файл с инструментами и зансим их в словарь в классе ExchangeComponents.MatchingEngine
        InputStream instrumentFile = new FileInputStream("src/main/java/ins.txt");
        ConcurrentHashMap<String, Instrument> instrumentHashMap = readInstrumentsFromInputStream(instrumentFile);
        MatchingEngine matchingEngine = new MatchingEngine(instrumentHashMap,traderMap);
        Notifier notifier = new Notifier(traderSSEClientMap, matchingEngine);
        JSONExchange jsonExchange = new JSONExchange(matchingEngine, traderBySseCode, instrumentHashMap, traderMap, traderSSEClientMap);

        Thread engineThread = new Thread(matchingEngine);
        Thread notifierThread = new Thread(notifier);
        engineThread.start();
        notifierThread.start();


        JAVALIN.post("/new-order-single", jsonExchange::createOrderFromJSON);
        JAVALIN.post("/order-cancel-request", jsonExchange::cancelOrderFromJSON);


        JAVALIN.post("/login",client ->{

            JSONObject loginData = new JSONObject(client.body());
            String id = loginData.getString("username");
            loginQueue.add(id);
            if(!traderMap.containsKey(id)) {

                Trader trader = new Trader(id);
                traderMap.put(id, trader);
                traderSSEClientMap.put(trader, new ArrayList<>());
            }


        });

        JAVALIN.post("/verify", client -> {
            JSONObject clientVerificationData = new JSONObject(client.body());
            String id = clientVerificationData.getString("name");
            String code = clientVerificationData.getString("code");
            if (loginQueue.contains(id)) {

                Client verifiedClient = clientAwaitingVerification.get(code);
                Trader temp = traderMap.get(id);
                temp.getAssociatedClients().add(verifiedClient);
                traderBySseCode.put(code, temp);
                traderSSEClientMap.get(temp).add(verifiedClient.getSseClient());
                verifiedClient.getSseClient().sendEvent("marketSnapshot", JSONConstructor.makeJSONMarketSnapshot(matchingEngine.instrumentMap).toString());
                temp.sendOrderEvent("receiveOrders", JSONConstructor.getOnesOrders(temp));
                temp.sendOrderEvent("receiveTrades", matchingEngine.constructTrades(temp));
            }


        }).sse("/sse", client -> {
            String code = getRandomNumber(0, 15000).toString();
            Client unverifiedClient = new Client(client, code);
            clientAwaitingVerification.put(code, unverifiedClient);
            client.sendEvent("connected", code);


        });


    }
    private static Integer getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);

    }

}
