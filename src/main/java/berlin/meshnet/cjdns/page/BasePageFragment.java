package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.os.Bundle;

import berlin.meshnet.cjdns.CjdnsApplication;

/**
 * The base class for pages.
 */
public abstract class BasePageFragment extends Fragment {

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((CjdnsApplication) getActivity().getApplication()).inject(this);
    }
}
