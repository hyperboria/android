package berlin.meshnet.cjdns.page;

import android.app.Fragment;
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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.ApplicationEvents;
import berlin.meshnet.cjdns.event.PeerEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.PeersProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.functions.Action1;

/**
 * The page representing the list of peers.
 */
public class PeersPageFragment extends BasePageFragment {

    @Inject
    Bus mBus;

    @Inject
    SettingsProducer mSettingsProducer;

    @Inject
    PeersProducer mPeersProducer;

    @InjectView(R.id.peers_page_recycler_view)
    RecyclerView mPeersRecyclerView;

    @InjectView(R.id.peers_page_add)
    FloatingActionButton mAdd;

    private PeerListAdapter mAdapter;

    public static Fragment newInstance() {
        return new PeersPageFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_peers_page, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPeersRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
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
        mPeersRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new PeerListAdapter(mBus,
                AppObservable.bindFragment(this, mSettingsProducer.themeStream()),
                AppObservable.bindFragment(this, mPeersProducer.createStream()),
                AppObservable.bindFragment(this, mPeersProducer.updateStream()),
                AppObservable.bindFragment(this, mPeersProducer.removeStream()));
        mPeersRecyclerView.setAdapter(mAdapter);

        IconDrawable addIcon = new IconDrawable(getActivity(), Iconify.IconValue.fa_plus)
                .colorRes(R.color.my_primary)
                .actionBarSize();
        addIcon.setStyle(Paint.Style.FILL);
        mAdd.setImageDrawable(addIcon);
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBus.post(new PeerEvents.Create());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mBus.register(mSettingsProducer);
        mBus.register(mPeersProducer);
    }

    @Override
    public void onPause() {
        mBus.unregister(mSettingsProducer);
        mBus.unregister(mPeersProducer);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mAdapter.onDestroyImpl();
        super.onDestroy();
    }

    private static class PeerListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final float ALPHA_ACTIVE = 1f;

        private static final float ALPHA_INACTIVE = 0.3f;

        private Bus mBus;

        private boolean mIsInternalsVisible;

        private List<Node.Peer> mPeers = new ArrayList<>();

        private List<Subscription> mSubscriptions = new ArrayList<>();

        private PeerListAdapter(Bus bus,
                                Observable<Theme> themeStream,
                                Observable<Node.Peer> createStream,
                                Observable<Node.Peer> updateStream,
                                Observable<Node.Peer> removeStream) {
            mBus = bus;

            mSubscriptions.add(themeStream.subscribe(new Action1<Theme>() {
                @Override
                public void call(Theme theme) {
                    mIsInternalsVisible = theme.isInternalsVisible;
                    notifyDataSetChanged();
                }
            }));

            mSubscriptions.add(createStream.subscribe(new Action1<Node.Peer>() {
                @Override
                public void call(Node.Peer peer) {
                    mPeers.add(peer);
                    notifyDataSetChanged();
                }
            }));

            mSubscriptions.add(updateStream.subscribe(new Action1<Node.Peer>() {
                @Override
                public void call(Node.Peer peer) {
                    int position = mPeers.indexOf(peer);
                    if (mPeers.remove(peer)) {
                        mPeers.add(position, peer);
                        notifyDataSetChanged();
                    }
                }
            }));

            mSubscriptions.add(removeStream.subscribe(new Action1<Node.Peer>() {
                @Override
                public void call(Node.Peer peer) {
                    int position = mPeers.indexOf(peer);
                    if (mPeers.remove(peer)) {
                        notifyItemRemoved(position);
                        notifyDataSetChanged();
                    }
                }
            }));
        }

        private void onDestroyImpl() {
            // Unsubscribe from observables.
            Iterator<Subscription> itr = mSubscriptions.iterator();
            while (itr.hasNext()) {
                itr.next().unsubscribe();
                itr.remove();
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.card_view_peer, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Node.Peer peer = mPeers.get(position);
            holder.name.setText(peer.name);
            holder.address.setText(peer.address);
            if (mIsInternalsVisible) {
                holder.publicKey.setText(peer.publicKey);
                holder.publicKeyContainer.setVisibility(View.VISIBLE);
            } else {
                holder.publicKeyContainer.setVisibility(View.GONE);
            }
            Credential[] outgoingConnections = peer.outgoingConnections;
            if (outgoingConnections != null && outgoingConnections.length > 0) {
                holder.connections.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBus.post(new ApplicationEvents.ListConnections(peer.id));
                    }
                });
                holder.connections.setVisibility(View.VISIBLE);
            } else {
                holder.connections.setVisibility(View.GONE);
            }
            holder.itemView.setAlpha(peer.stats.isActive ? ALPHA_ACTIVE : ALPHA_INACTIVE);
        }

        @Override
        public int getItemCount() {
            return mPeers.size();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        @InjectView(R.id.peer_card_name)
        TextView name;

        @InjectView(R.id.peer_card_address)
        TextView address;

        @InjectView(R.id.peer_card_public_key_container)
        LinearLayout publicKeyContainer;

        @InjectView(R.id.peer_card_public_key)
        TextView publicKey;

        @InjectView(R.id.peer_card_connections)
        IconTextView connections;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
