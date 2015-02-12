package berlin.meshnet.cjdns.producer;

import android.content.Context;
import android.content.SharedPreferences;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import berlin.meshnet.cjdns.R;
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

    /**
     * Verbose implementation of a {@link berlin.meshnet.cjdns.producer.ThemeProducer}.
     */
    public static class Default extends ThemeProducer {

        private Context mContext;

        private SharedPreferences mSharedPreferences;

        public Default(Context context, SharedPreferences sharedPreferences, Bus bus) {
            super(bus);
            mContext = context;
            mSharedPreferences = sharedPreferences;
        }

        @Override
        @Produce
        public Theme produce() {
            return new Theme(mSharedPreferences.getBoolean(mContext.getString(R.string.setting_verbose_enabled_key), false));
        }
    }
}
