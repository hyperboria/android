package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;

import berlin.meshnet.cjdns.CjdnsApplication;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String encryptEnabledKey = getString(R.string.setting_encrypt_enabled_key);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit();
        Preference preference = getPreferenceManager().findPreference(encryptEnabledKey);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager != null) {
            int encryptionStatus = devicePolicyManager.getStorageEncryptionStatus();
            switch (encryptionStatus) {
                case DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE:
                    editor.putInt(encryptEnabledKey, encryptionStatus).apply();
                    preference.setSummary(R.string.settings_page_setting_encrypt_summary_disabled);
                    preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                            return true;
                        }
                    });
                    preference.setEnabled(true);
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING:
                case DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE:
                    editor.putInt(encryptEnabledKey, encryptionStatus).apply();
                    preference.setSummary(R.string.settings_page_setting_encrypt_summary_enabled);
                    preference.setEnabled(false);
                    break;
                case DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED:
                default:
                    editor.putInt(encryptEnabledKey, DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED).apply();
                    preference.setSummary(R.string.settings_page_setting_encrypt_summary_unsupported);
                    preference.setEnabled(false);
            }
        } else {
            editor.putInt(encryptEnabledKey, DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED).apply();
            preference.setSummary(R.string.settings_page_setting_encrypt_summary_unsupported);
            preference.setEnabled(false);
        }
    }
}
