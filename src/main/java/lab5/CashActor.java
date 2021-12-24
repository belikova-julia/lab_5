package lab5;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class CashActor extends AbstractActor {
    private final Map<String, Float> cash = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StoreMessage.class,
                        req -> cash.put(req.getUrl(), req.getAvgTime()))
                .match(String.class,
                        req -> sender().tell(cash.getOrDefault(req, (float)-1.0), ActorRef.noSender()))
                .build();
    }
}
