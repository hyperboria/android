package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewFragment;
import android.widget.TextView;

import java.util.regex.Pattern;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;
import butterknife.ButterKnife;

/**
 * The page explaining what the application is about.
 */
public class AboutPageFragment extends BasePageFragment {

    private View mView;

    public static Fragment newInstance() {
        return new AboutPageFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.inject(this, mView);
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView version = (TextView) mView.findViewById(R.id.version);
        TextView ln1 = (TextView) mView.findViewById(R.id.ln1);

        Pattern pattern = Pattern.compile("https://github.com/hyperboria/android");
        Linkify.addLinks(ln1, pattern, "");

        String versionString = "-.-.-";
        try {
            versionString = getActivity().getApplicationContext().getPackageManager().getPackageInfo(
                    getActivity().getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        version.setText(getString(R.string.version) + " " + versionString);
    }
}
