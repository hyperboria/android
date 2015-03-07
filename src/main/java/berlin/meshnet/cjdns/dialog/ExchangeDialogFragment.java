package berlin.meshnet.cjdns.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.ApplicationEvents;

/**
 * Dialog that facilitates the exchange of credentials across different medium.
 */
public class ExchangeDialogFragment extends DialogFragment {

    private static final String FRAGMENT_BUNDLE_KEY_TITLE_RES_ID = "titleResId";

    private static final String FRAGMENT_BUNDLE_KEY_OPTIONS_RES_ID = "optionsResId";

    private static final String FRAGMENT_BUNDLE_KEY_MESSAGE = "message";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final int titleResId = args.getInt(FRAGMENT_BUNDLE_KEY_TITLE_RES_ID);
        final int optionsResId = args.getInt(FRAGMENT_BUNDLE_KEY_OPTIONS_RES_ID);
        final String message = args.getString(FRAGMENT_BUNDLE_KEY_MESSAGE);

        final String[] options = getResources().getStringArray(optionsResId);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(titleResId)
                .adapter(new ArrayAdapter<>(getActivity(), R.layout.view_exchange_item, options))
                .build();

        ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // TODO
                    String text = options[position] + "\n" + message;
                    Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();

                    ExchangeDialogFragment.this.dismiss();
                }
            });
        }

        return dialog;
    }

    public static DialogFragment newInstance(ApplicationEvents.ExchangeCredential.Type type, String message) {
        DialogFragment fragment = new ExchangeDialogFragment();

        int titleResId;
        int optionsResId;
        switch (type) {
            case broadcast:
                titleResId = R.string.credential_broadcast_share_title;
                optionsResId = R.array.credential_broadcast_share_exchange_options;
                break;
            case target:
            default:
                titleResId = R.string.credential_target_share_title;
                optionsResId = R.array.credential_target_share_exchange_options;
        }

        Bundle args = new Bundle();
        args.putInt(FRAGMENT_BUNDLE_KEY_TITLE_RES_ID, titleResId);
        args.putInt(FRAGMENT_BUNDLE_KEY_OPTIONS_RES_ID, optionsResId);
        args.putString(FRAGMENT_BUNDLE_KEY_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }
}
