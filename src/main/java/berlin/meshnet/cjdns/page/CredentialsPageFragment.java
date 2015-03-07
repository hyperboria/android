package berlin.meshnet.cjdns.page;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.ApplicationEvents;
import berlin.meshnet.cjdns.event.AuthorizedCredentialEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Protocol;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.CredentialsProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
import brnunes.swipeablecardview.SwipeableRecyclerViewTouchListener;
import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.functions.Action1;

/**
 * The page representing the list of credentials authorized credentials.
 */
public class CredentialsPageFragment extends BasePageFragment {

    @Inject
    Bus mBus;

    @Inject
    SettingsProducer mSettingsProducer;

    @Inject
    CredentialsProducer mCredentialProducer;

    @InjectView(R.id.credentials_page_recycler_view)
    RecyclerView mCredentialsRecyclerView;

    @InjectView(R.id.credentials_page_add)
    FloatingActionButton mAdd;

    private CredentialListAdapter mAdapter;

    public static Fragment newInstance() {
        return new CredentialsPageFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credentials_page, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCredentialsRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int childPosition = parent.getChildPosition(view);
                if (childPosition == 0) {
                    outRect.top = view.getResources().getDimensionPixelSize(R.dimen.global_margin);
                }
                if (childPosition == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = view.getResources().getDimensionPixelSize(R.dimen.global_margin);
                }
            }
        });
        mCredentialsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new CredentialListAdapter(getActivity(), mBus,
                AppObservable.bindFragment(this, mSettingsProducer.themeStream()),
                AppObservable.bindFragment(this, mCredentialProducer.createStream()),
                AppObservable.bindFragment(this, mCredentialProducer.updateStream()),
                AppObservable.bindFragment(this, mCredentialProducer.removeStream()));
        mCredentialsRecyclerView.setAdapter(mAdapter);
        mCredentialsRecyclerView.addOnItemTouchListener(new SwipeableRecyclerViewTouchListener(mCredentialsRecyclerView,
                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipe(int position) {
                        return !mAdapter.getItem(position).isAllowed;
                    }

                    @Override
                    public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            mBus.post(new AuthorizedCredentialEvents.Remove(mAdapter.getItem(position)));
                        }
                    }

                    @Override
                    public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        for (int position : reverseSortedPositions) {
                            mBus.post(new AuthorizedCredentialEvents.Remove(mAdapter.getItem(position)));
                        }
                    }
                }));

        IconDrawable addIcon = new IconDrawable(getActivity(), Iconify.IconValue.fa_plus)
                .colorRes(R.color.my_primary)
                .actionBarSize();
        addIcon.setStyle(Paint.Style.FILL);
        mAdd.setImageDrawable(addIcon);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBus.post(new AuthorizedCredentialEvents.Create());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mBus.register(mSettingsProducer);
        mBus.register(mCredentialProducer);
    }

    @Override
    public void onPause() {
        mBus.unregister(mSettingsProducer);
        mBus.unregister(mCredentialProducer);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mAdapter.onDestroyImpl();
        super.onDestroy();
    }

    private static class CredentialListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final float ALPHA_ALLOWED = 1f;

        private static final float ALPHA_REVOKED = 0.3f;

        private Resources mResources;

        private Bus mBus;

        private boolean mIsInternalsVisible;

        private List<Credential.Authorized> mCredentials = new ArrayList<>();

        private List<Subscription> mSubscriptions = new ArrayList<>();

        private CredentialListAdapter(Context context, Bus bus,
                                      Observable<Theme> themeStream,
                                      Observable<Credential.Authorized> createStream,
                                      Observable<Credential.Authorized> updateStream,
                                      Observable<Credential.Authorized> removeStream) {
            mResources = context.getApplicationContext().getResources();
            mBus = bus;

            mSubscriptions.add(themeStream.subscribe(new Action1<Theme>() {
                @Override
                public void call(Theme theme) {
                    mIsInternalsVisible = theme.isInternalsVisible;
                    notifyDataSetChanged();
                }
            }));

            mSubscriptions.add(createStream.subscribe(new Action1<Credential.Authorized>() {
                @Override
                public void call(Credential.Authorized credential) {
                    mCredentials.add(credential);
                    notifyDataSetChanged();
                }
            }));

            mSubscriptions.add(updateStream.subscribe(new Action1<Credential.Authorized>() {
                @Override
                public void call(Credential.Authorized credential) {
                    int position = mCredentials.indexOf(credential);
                    if (mCredentials.remove(credential)) {
                        mCredentials.add(position, credential);
                        notifyDataSetChanged();
                    }
                }
            }));

            mSubscriptions.add(removeStream.subscribe(new Action1<Credential.Authorized>() {
                @Override
                public void call(Credential.Authorized credential) {
                    int position = mCredentials.indexOf(credential);
                    if (mCredentials.remove(credential)) {
                        notifyItemRemoved(position);
                        notifyDataSetChanged();
                    }
                }
            }));
        }

        private Credential.Authorized getItem(int position) {
            return mCredentials.get(position);
        }

        private void onDestroyImpl() {
            for (Subscription subscription : mSubscriptions) {
                subscription.unsubscribe();
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_view_credential, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Credential.Authorized credential = mCredentials.get(position);
            holder.label.setText(credential.label);
            holder.protocol.setText(Protocol.formatDescription(mResources, credential.protocol));
            if (mIsInternalsVisible) {
                holder.password.setText(credential.password);
                holder.passwordContainer.setVisibility(View.VISIBLE);
            } else {
                holder.passwordContainer.setVisibility(View.GONE);
            }
            holder.broadcast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = "http://wrbt.hyperboria.net?type=credentials" +
                            "&interface=" + credential.protocol.transportInterface +
                            "&link=" + credential.protocol.link +
                            "&message=" + credential.label + "+" + credential.password;
                    mBus.post(new ApplicationEvents.ExchangeCredential(ApplicationEvents.ExchangeCredential.Type.broadcast, message));
                }
            });
            holder.target.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = "http://wrbt.hyperboria.net?type=credentials" +
                            "&interface=" + credential.protocol.transportInterface +
                            "&link=" + credential.protocol.link +
                            "&message=" + credential.label + "+" + credential.password;
                    mBus.post(new ApplicationEvents.ExchangeCredential(ApplicationEvents.ExchangeCredential.Type.target, message));
                }
            });
            holder.allow.setText(credential.isAllowed
                    ? mResources.getString(R.string.credential_card_allow_button_on)
                    : mResources.getString(R.string.credential_card_allow_button_off));
            holder.allow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Credential.Authorized update = new Credential.Authorized(credential.id,
                            credential.label, credential.protocol, credential.password, !credential.isAllowed);
                    mBus.post(new AuthorizedCredentialEvents.Update(update));
                }
            });
            holder.itemView.setAlpha(credential.isAllowed ? ALPHA_ALLOWED : ALPHA_REVOKED);
        }

        @Override
        public int getItemCount() {
            return mCredentials.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.credential_card_label)
        TextView label;

        @InjectView(R.id.credential_card_protocol)
        TextView protocol;

        @InjectView(R.id.credential_card_password_container)
        LinearLayout passwordContainer;

        @InjectView(R.id.credential_card_password)
        TextView password;

        @InjectView(R.id.credential_card_broadcast)
        IconTextView broadcast;

        @InjectView(R.id.credential_card_target)
        IconTextView target;

        @InjectView(R.id.credential_card_allow)
        IconTextView allow;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
