package berlin.meshnet.cjdns.event;

public class ExchangeEvent {

    public final Type mType;

    public ExchangeEvent(Type type) {
        mType = type;
    }

    public enum Type {
        target,
        broadcast
    }
}
