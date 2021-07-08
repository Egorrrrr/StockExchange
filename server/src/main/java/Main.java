import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        OrderEntryGateway orderEntryGateway = new OrderEntryGateway(3001);
        orderEntryGateway.start();


    }
}
