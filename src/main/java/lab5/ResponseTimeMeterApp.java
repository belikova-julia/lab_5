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
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.asynchttpclient.AsyncHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class ResponseTimeMeterApp {
    private static final int PORT = 8080;
    private static final String HOST = "localhost";
    private static final String SYSTEM_NAME = "timer";

    private static final String START_MESSAGE = "Start";
    private static final String START_INFO_FORMAT = "Server online at http://%s:%d/";

    private static String REQUEST_TEST_URL = "testUrl";
    private static String REQUEST_COUNT = "count";

    private static int MAP_PARALLEL = 2;
    private static Duration TIMEOUT = Duration.ofSeconds(5);


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
                        req -> Patterns
                                .ask(cash, req.first(), TIMEOUT)
                                .thenCompose(t -> {
                                    if ((float)t >= 0)
                                        return CompletableFuture.completedFuture(new Pair<>(req.first(), (float)t));
                                    return Source.from(Collections.singletonList(req))
                                            .toMat(testSink, Keep.right())
                                            .run(materializer);

                                }));
    }

    private static Sink<Pair<String, Integer>, CompletionStage<Long>> createSink(int reqNumber) {
        return Flow.<Pair<String, Integer>>create()
                .mapConcat(p -> new ArrayList<>(Collections.nCopies(p.second(), p.first())))
                .mapAsync(reqNumber, url -> {
                    AsyncHttpClient client = asyncHttpClient();
                    long startTime = System.currentTimeMillis();
                    client.prepareGet(url).execute();
                    long finalTime = System.currentTimeMillis() - startTime;
                    return CompletableFuture.completedFuture(finalTime);
                })
                .toMat(Sink.fold(0L, Long::sum), Keep.right());
    }
}
