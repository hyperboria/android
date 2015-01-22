package berlin.meshnet.cjdns.producer;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import berlin.meshnet.cjdns.model.Theme;

/**
 * Abstract class that produces {@link berlin.meshnet.cjdns.model.Theme}.
 */
public abstract class ThemeProducer {

    public ThemeProducer(Bus bus) {
        bus.register(this);
    }

    /**
     * Produces {@link berlin.meshnet.cjdns.model.Theme} to any subscribers. Must be annotated with {@link @Produce}.
     *
     * @return A {@link berlin.meshnet.cjdns.model.Theme}.
     */
    public abstract Theme produce();

    /**
     * Minimalist implementation of a {@link berlin.meshnet.cjdns.producer.ThemeProducer}.
     */
    public static class MinimalMock extends ThemeProducer {

        public MinimalMock(Bus bus) {
            super(bus);
        }

        @Override
        @Produce
        public Theme produce() {
            return new Theme(false);
        }
    }

    /**
     * Verbose implementation of a {@link berlin.meshnet.cjdns.producer.ThemeProducer}.
     */
    public static class VerboseMock extends ThemeProducer {

        public VerboseMock(Bus bus) {
            super(bus);
        }

        @Override
        @Produce
        public Theme produce() {
            return new Theme(true);
        }
    }
}
