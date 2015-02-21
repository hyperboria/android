package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.os.Bundle;
import android.webkit.WebViewFragment;

/**
 * The page explaining what the application is about.
 */
public class AboutPageFragment extends WebViewFragment {

    private static final String ABOUT_URL = "http://hyperboria.github.io/cjdns-android/";

    public static Fragment newInstance() {
        return new AboutPageFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getWebView().loadUrl(ABOUT_URL);
    }
}
