package berlin.meshnet.cjdns.page;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.MeProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
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
    SettingsProducer mSettingsProducer;

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

        mSubscriptions.add(AppObservable.bindFragment(this, mSettingsProducer.themeStream())
                .subscribe(new Action1<Theme>() {
                    @Override
                    public void call(Theme theme) {
                        mPublicKey.setVisibility(theme.isInternalsVisible ? View.VISIBLE : View.GONE);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // TODO
                    }
                }));

        mSubscriptions.add(AppObservable.bindFragment(this, mMeProducer.stream(getContext()))
                .subscribe(new Action1<Node.Me>() {
                    @Override
                    public void call(Node.Me me) {
                        mNameTextView.setText(me.name);
                        mAddressTextView.setText(me.address);
                        mPublicKeyTextView.setText(me.publicKey);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        // TODO
                    }
                }));

        // Share address on click.
        mAddressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMeProducer.stream(getContext())
                        .subscribe(new Action1<Node.Me>() {
                            @Override
                            public void call(Node.Me me) {
                                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "cjdns IPv6");
                                intent.putExtra(android.content.Intent.EXTRA_TEXT, me.address);
                                startActivity(Intent.createChooser(intent, "Share using..."));
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                // TODO
                            }
                        });
            }
        });
    }

    @Override
    public void onDestroy() {
        // Unsubscribe from observables.
        Iterator<Subscription> itr = mSubscriptions.iterator();
        while (itr.hasNext()) {
            itr.next().unsubscribe();
            itr.remove();
        }

        super.onDestroy();
    }
}
