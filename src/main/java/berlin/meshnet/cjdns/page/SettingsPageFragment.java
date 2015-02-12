package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import berlin.meshnet.cjdns.R;

/**
 * The page to configure application settings.
 */
public class SettingsPageFragment extends PreferenceFragment {

    public static Fragment newInstance() {
        return new SettingsPageFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
