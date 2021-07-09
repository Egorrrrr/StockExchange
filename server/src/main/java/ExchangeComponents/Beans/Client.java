package ExchangeComponents.Beans;

import io.javalin.http.sse.SseClient;

public class Client {

    private SseClient sseClient;

    public SseClient getSseClient() {
        return sseClient;
    }

    private String code;

    public String getCode() {
        return code;
    }


    public Client(SseClient sseClient, String code){

        this.sseClient = sseClient;
        this.code = code;


    }
}
