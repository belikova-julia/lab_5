package lab5;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class ResponseTimeMeterApp {
    private static final int PORT = 8080;
    private static final String HOST = "localhost";
    private static final String SYSTEM_NAME = "timer";

    private static final String START_MESSAGE = "Start";
    private static final String START_INFO_FORMAT = "Server online at http://%s:%d/";

    private static String REQUEST_TEST_URL = "testUrl";
    private static String REQUEST_COUNT = "count";
    private static int MAP_PARALLEL = 2;


    public static void main(String[] args) throws IOException {
        System.out.println(START_MESSAGE);
        ActorSystem system = ActorSystem.create(SYSTEM_NAME);
        ActorRef cash = system.actorOf(Props.create(CashActor.class), "cash");
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(materializer, cash);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(HOST, PORT),
                materializer
        );
        System.out.printf(START_INFO_FORMAT, HOST, PORT);
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());
    }

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(ActorMaterializer materializer, ActorRef cash) {
        return Flow.of(HttpRequest.class)
                .map(req -> {
                    Query query = req.getUri().query();
                    return new Pair<>(
                            query.get(REQUEST_TEST_URL).get(),
                            Integer.parseInt(query.get(REQUEST_COUNT).get())
                    );
                })
                .mapAsync(
                        MAP_PARALLEL,
                        (Pair<String, Integer> req) ->
                                Patterns.ask(cash, req.first(), ))
    }
}
