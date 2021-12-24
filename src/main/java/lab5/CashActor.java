package lab5;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class CashActor extends AbstractActor {
    private final Map<String, Float> cash = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StoreMessage.class, req -> cash.put(req.getUrl(), req.getAvgTime()))
                .match()
                .build()
    }
}
