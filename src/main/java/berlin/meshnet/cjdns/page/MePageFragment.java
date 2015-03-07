package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.ThemeProducer;
import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.functions.Action1;

/**
 * The page representing the self node.
 */
public class MePageFragment extends BasePageFragment {

    @Inject
    ThemeProducer mThemeProducer;

    @Inject
    MeProducer mMeProducer;

    @InjectView(R.id.me_page_name_text)
    TextView mNameTextView;

    @InjectView(R.id.me_page_address_text)
    TextView mAddressTextView;

    @InjectView(R.id.me_page_public_key)
    LinearLayout mPublicKey;

    @InjectView(R.id.me_page_public_key_text)
    TextView mPublicKeyTextView;

    private List<Subscription> mSubscriptions = new ArrayList<>();

    public static Fragment newInstance() {
        return new MePageFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me_page, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSubscriptions.add(AppObservable.bindFragment(this, mThemeProducer.stream())
                .subscribe(new Action1<Theme>() {
                    @Override
                    public void call(Theme theme) {
                        mPublicKey.setVisibility(theme.isInternalsVisible ? View.VISIBLE : View.GONE);
                    }
                }));

        mSubscriptions.add(AppObservable.bindFragment(this, mMeProducer.stream())
                .subscribe(new Action1<Node.Me>() {
                    @Override
                    public void call(Node.Me me) {
                        mNameTextView.setText(me.name);
                        mAddressTextView.setText(me.address);
                        mPublicKeyTextView.setText(me.publicKey);
                    }
                }));
    }

    @Override
    public void onDestroy() {
        for (Subscription subscription : mSubscriptions) {
            subscription.unsubscribe();
        }
        super.onDestroy();
    }
}
