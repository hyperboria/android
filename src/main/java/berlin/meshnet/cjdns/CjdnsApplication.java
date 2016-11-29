package berlin.meshnet.cjdns;

import android.app.Application;
import android.preference.PreferenceManager;

import berlin.meshnet.cjdns.dagger.CoreComponent;
import berlin.meshnet.cjdns.dagger.DaggerCoreComponent;
import berlin.meshnet.cjdns.dagger.DefaultModule;

/**
 * The {@link android.app.Application}.
 */
public class CjdnsApplication extends Application {

    CoreComponent mCoreComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set module to provide dependencies and build core component.
        mCoreComponent = DaggerCoreComponent.builder()
                .defaultModule(new DefaultModule(this))
                .build();

        // Set default values for user preferences.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    /**
     * Gets the {@link CoreComponent} holding the all dependencies.
     */
    public CoreComponent getComponent() {
        return mCoreComponent;
    }
}