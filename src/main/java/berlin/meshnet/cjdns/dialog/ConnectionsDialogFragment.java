package berlin.meshnet.cjdns.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.IconTextView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.PeerEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Protocol;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.PeersProducer;
import berlin.meshnet.cjdns.producer.SettingsProducer;
import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Dialog that facilitates the management of outgoing connections to a peer node.
 */
public class ConnectionsDialogFragment extends DialogFragment {

    private static final String FRAGMENT_BUNDLE_KEY_PEER_ID = "peerId";

    @Inject
    Bus mBus;

    @Inject
    SettingsProducer mSettingsProducer;

    @Inject
    PeersProducer mPeersProducer;

    private ConnectionAdapter mAdapter;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ((CjdnsApplication) getActivity().getApplication()).inject(this);

        Bundle args = getArguments();
        final int peerId = args.getInt(FRAGMENT_BUNDLE_KEY_PEER_ID);

        final Observable<Node.Peer> peerStream = mPeersProducer.createStream()
                .mergeWith(mPeersProducer.updateStream())
                .filter(new Func1<Node.Peer, Boolean>() {
                    @Override
                    public Boolean call(Node.Peer peer) {
                        return peer.id == peerId;
                    }
                });

        mAdapter = new ConnectionAdapter(getActivity(), mBus,
                AppObservable.bindFragment(this, mSettingsProducer.themeStream()),
                AppObservable.bindFragment(this, peerStream));
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (mAdapter.hasData() && mAdapter.getCount() <= 0) {
                    dismiss();
                }
            }
        });

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.connections_list_title)
                .adapter(mAdapter, null)
                .listSelector(R.drawable.md_transparent)
                .build();
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

    public static DialogFragment newInstance(int peerId) {
        DialogFragment fragment = new ConnectionsDialogFragment();

        Bundle args = new Bundle();
        args.putInt(FRAGMENT_BUNDLE_KEY_PEER_ID, peerId);
        fragment.setArguments(args);

        return fragment;
    }

    private static class ConnectionAdapter extends BaseAdapter {

        private Resources mRes;

        private LayoutInflater mInflater;

        private Bus mBus;

        private Node.Peer mPeer;

        private boolean mIsInternalsVisible;

        private List<Subscription> mSubscriptions = new ArrayList<>();

        private ConnectionAdapter(Context context, Bus bus,
                                  Observable<Theme> themeStream,
                                  Observable<Node.Peer> peerStream) {
            mRes = context.getResources();
            mInflater = LayoutInflater.from(context);
            mBus = bus;

            mSubscriptions.add(themeStream.subscribe(new Action1<Theme>() {
                @Override
                public void call(Theme theme) {
                    mIsInternalsVisible = theme.isInternalsVisible;
                    notifyDataSetChanged();
                }
            }));

            mSubscriptions.add(peerStream.subscribe(new Action1<Node.Peer>() {
                @Override
                public void call(Node.Peer peer) {
                    mPeer = peer;
                    notifyDataSetChanged();
                }
            }));
        }

        private boolean hasData() {
            return mPeer != null;
        }

        private void onDestroyImpl() {
            for (Subscription subscription : mSubscriptions) {
                subscription.unsubscribe();
            }
        }

        @Override
        public int getCount() {
            if (mPeer == null || mPeer.outgoingConnections == null) {
                return 0;
            }
            return mPeer.outgoingConnections.length;
        }

        @Override
        public Credential getItem(int position) {
            if (mPeer == null || mPeer.outgoingConnections == null) {
                return null;
            }
            return mPeer.outgoingConnections[position];
        }

        @Override
        public long getItemId(int position) {
            if (mPeer == null || mPeer.outgoingConnections == null) {
                return -1L;
            }
            return mPeer.outgoingConnections[position].id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = mInflater.inflate(R.layout.view_connection_item, null);
            }

            final Credential credential = getItem(position);
            ViewHolder holder = new ViewHolder(itemView);
            holder.label.setText(credential.label);
            holder.protocol.setText(mRes.getString(R.string.connections_list_item_protocol, Protocol.formatDescription(mRes, credential.protocol)));
            if (mIsInternalsVisible) {
                holder.password.setText(mRes.getString(R.string.connections_list_item_password, credential.password));
                holder.password.setVisibility(View.VISIBLE);
            } else {
                holder.password.setVisibility(View.GONE);
            }
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Credential> connections = new ArrayList<>(Arrays.asList(mPeer.outgoingConnections));
                    connections.remove(credential);
                    Node.Peer update = new Node.Peer(mPeer.id, mPeer.name, mPeer.publicKey,
                            connections.toArray(new Credential[connections.size()]));
                    mBus.post(new PeerEvents.Update(update));
                }
            });

            return itemView;
        }
    }

    static class ViewHolder {

        @InjectView(R.id.connection_item_label)
        TextView label;

        @InjectView(R.id.connection_item_protocol)
        IconTextView protocol;

        @InjectView(R.id.connection_item_password)
        IconTextView password;

        @InjectView(R.id.connection_item_delete)
        IconTextView delete;

        public ViewHolder(View itemView) {
            ButterKnife.inject(this, itemView);
        }
    }
}
