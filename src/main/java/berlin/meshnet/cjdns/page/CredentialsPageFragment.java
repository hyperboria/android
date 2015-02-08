package berlin.meshnet.cjdns.page;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.CredentialEvents;
import berlin.meshnet.cjdns.event.ExchangeEvent;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.CredentialListProducer;
import berlin.meshnet.cjdns.producer.ThemeProducer;
import brnunes.swipeablecardview.SwipeableRecyclerViewTouchListener;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * The page representing the list of credentials known to the self node.
 */
public class CredentialsPageFragment extends Fragment {

    @Inject
    Bus mBus;

    @Inject
    ThemeProducer mThemeProducer;

    @Inject
    CredentialListProducer mCredentialListProducer;

    @InjectView(R.id.credentials_page_recycler_view)
    RecyclerView mCredentialsRecyclerView;

    @InjectView(R.id.credentials_page_add)
    FloatingActionButton mAdd;

    private Boolean mIsInternalsVisible = null;

    private CredentialListProducer.CredentialList mCredentialList = null;

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
        ((CjdnsApplication) getActivity().getApplication()).inject(this);
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

        IconDrawable addIcon = new IconDrawable(getActivity(), Iconify.IconValue.fa_plus)
                .colorRes(R.color.my_primary)
                .actionBarSize();
        addIcon.setStyle(Paint.Style.FILL);
        mAdd.setImageDrawable(addIcon);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBus.post(new CredentialEvents.Create());
            }
        });
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

    @Subscribe
    public void handleTheme(Theme theme) {
        mIsInternalsVisible = theme.isInternalsVisible;
        loadCredentialList();
    }

    @Subscribe
    public void handleCredentialList(CredentialListProducer.CredentialList credentialList) {
        mCredentialList = credentialList;
        loadCredentialList();
    }

    @Subscribe
    public void handleNewCredential(CredentialEvents.New event) {
        if (mCredentialList != null) {
            mCredentialList.add(event.mCredential);
            RecyclerView.Adapter adapter = mCredentialsRecyclerView.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Loads the list of credentials.
     */
    private void loadCredentialList() {
        if (mCredentialList != null && mIsInternalsVisible != null) {
            final RecyclerView.Adapter adapter = new CredentialListAdapter(getActivity(), mBus, mCredentialList, mIsInternalsVisible);
            mCredentialsRecyclerView.setAdapter(adapter);
            mCredentialsRecyclerView.addOnItemTouchListener(new SwipeableRecyclerViewTouchListener(mCredentialsRecyclerView,
                    new SwipeableRecyclerViewTouchListener.SwipeListener() {
                        @Override
                        public boolean canSwipe(int position) {
                            return !mCredentialList.get(position).isAllowed();
                        }

                        @Override
                        public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                            for (int position : reverseSortedPositions) {
                                int credentialId = mCredentialList.get(position).id;
                                mCredentialList.remove(position);
                                adapter.notifyItemRemoved(position);
                                mBus.post(new CredentialEvents.Remove(credentialId));
                            }
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                            for (int position : reverseSortedPositions) {
                                int credentialId = mCredentialList.get(position).id;
                                mCredentialList.remove(position);
                                adapter.notifyItemRemoved(position);
                                mBus.post(new CredentialEvents.Remove(credentialId));
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }));
        }
    }

    static class CredentialListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final float ALPHA_ALLOWED = 1f;

        private static final float ALPHA_REVOKED = 0.3f;

        private Resources mResources;

        private Bus mBus;

        private CredentialListProducer.CredentialList mCredentialList;

        private boolean mIsInternalsVisible;

        private CredentialListAdapter(Context context, Bus bus, CredentialListProducer.CredentialList credentialList,
                                      boolean isInternalsVisible) {
            mResources = context.getApplicationContext().getResources();
            mBus = bus;
            mCredentialList = credentialList;
            mIsInternalsVisible = isInternalsVisible;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_view_credential, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Credential credential = mCredentialList.get(position);
            holder.label.setText(credential.label);
            holder.protocol.setText(credential.protocol.getDescription(mResources));
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
                    mBus.post(new ExchangeEvent(ExchangeEvent.Type.broadcast, message));
                }
            });
            holder.target.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = "http://wrbt.hyperboria.net?type=credentials" +
                            "&interface=" + credential.protocol.transportInterface +
                            "&link=" + credential.protocol.link +
                            "&message=" + credential.label + "+" + credential.password;
                    mBus.post(new ExchangeEvent(ExchangeEvent.Type.target, message));
                }
            });
            holder.allow.setText(credential.isAllowed()
                    ? mResources.getString(R.string.credential_card_allow_button_on)
                    : mResources.getString(R.string.credential_card_allow_button_off));
            holder.allow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    credential.setAllowed(!credential.isAllowed());
                    mBus.post(new CredentialEvents.Update(credential));
                    notifyDataSetChanged();
                }
            });
            holder.itemView.setAlpha(credential.isAllowed() ? ALPHA_ALLOWED : ALPHA_REVOKED);
        }

        @Override
        public int getItemCount() {
            return mCredentialList.size();
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
