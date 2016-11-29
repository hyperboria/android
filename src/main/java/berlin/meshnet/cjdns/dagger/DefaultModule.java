package berlin.meshnet.cjdns.dagger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.squareup.otto.Bus;

import java.net.UnknownHostException;

import javax.inject.Singleton;

import berlin.meshnet.cjdns.Cjdroute;
import berlin.meshnet.cjdns.producer.CredentialsProducer;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.PeersProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
import dagger.Module;
import dagger.Provides;

/**
 * {@link dagger.Module} providing default dependencies.
 */
@Module
public class DefaultModule {

    private Context mContext;

    public DefaultModule(Context context) {
        mContext = context;
    }

    @Singleton
    @Provides
    Context provideContext() {
        return mContext;
    }

    @Singleton
    @Provides
    SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Singleton
    @Provides
    Bus provideBus() {
        return new Bus();
    }

    @Singleton
    @Provides
    Cjdroute provideCjdroute(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new Cjdroute.Compat(context.getApplicationContext());
        }
        return new Cjdroute.Default(context.getApplicationContext());
    }

    @Provides
    SettingsProducer provideSettingsProducer(Context context, SharedPreferences sharedPreferences) {
        return new SettingsProducer.Default(context, sharedPreferences);
    }

    @Provides
    MeProducer provideMeProducer() {
        return new MeProducer.Default();
    }

    @Provides
    PeersProducer providePeerListProducer() {
        try {
            return new PeersProducer.Default();
        } catch (UnknownHostException e) {
            // TODO
        }
        return null;
    }

    @Provides
    CredentialsProducer provideCredentialListProducer() {
        return new CredentialsProducer.Mock();
    }
}
