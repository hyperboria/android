package berlin.meshnet.cjdns.page;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.io.File;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;

/**
 * The page to configure application settings.
 */
public class SettingsPageFragment extends PreferenceFragmentCompat {

    private static final String TYPE_APK = "image/apk";

    @Inject
    SharedPreferences mSharedPreferences;

    public static Fragment newInstance() {
        return new SettingsPageFragment();
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((CjdnsApplication) getActivity().getApplication()).inject(this);

        String encryptEnabledKey = getString(R.string.setting_encrypt_enabled_key);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        Preference preference = getPreferenceManager().findPreference(encryptEnabledKey);
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
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

        String sendApkKey = getString(R.string.setting_send_apk_key);
        getPreferenceManager().findPreference(sendApkKey)
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        File apk = new File(getActivity().getApplicationInfo().sourceDir);
                        startActivity(new Intent(Intent.ACTION_SEND)
                                .putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apk))
                                .setType(TYPE_APK)
                                .addCategory(Intent.CATEGORY_DEFAULT)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        return true;
                    }
                });
    }
}
