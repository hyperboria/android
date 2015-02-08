package berlin.meshnet.cjdns.event;

public class ExchangeEvent {

    public final Type mType;

    public final String mMessage;

    public ExchangeEvent(Type type, String message) {
        mType = type;
        mMessage = message;
    }

    public enum Type {
        target,
        broadcast
    }
}
