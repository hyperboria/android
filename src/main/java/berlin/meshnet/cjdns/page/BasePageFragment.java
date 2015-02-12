package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.os.Bundle;

import com.squareup.otto.Bus;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;

/**
 * The base class for pages.
 */
public abstract class BasePageFragment extends Fragment {

    @Inject
    Bus mBus;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((CjdnsApplication) getActivity().getApplication()).inject(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBus.register(this);
    }

    @Override
    public void onPause() {
        mBus.unregister(this);
        super.onPause();
    }
}
