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
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.IconTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.event.PeerEvents;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Node;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.PeerListProducer;
import berlin.meshnet.cjdns.producer.ThemeProducer;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Dialog that facilitates the management of outgoing connections to a peer node.
 */
public class ConnectionsDialogFragment extends DialogFragment {

    private static final String FRAGMENT_BUNDLE_KEY_PEER_ID = "peerId";

    @Inject
    Bus mBus;

    @Inject
    ThemeProducer mThemeProducer;

    @Inject
    PeerListProducer mPeerListProducer;

    private int mPeerId;

    private Boolean mIsInternalsVisible = null;

    private Node.Peer mPeer = null;

    private ListView mListView = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mPeerId = args.getInt(FRAGMENT_BUNDLE_KEY_PEER_ID);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.connections_list_title)
                .adapter(new ConnectionAdapter(getActivity(), mBus, null, false))
                .dividerColor(R.color.material_blue_500)
                .build();
        mListView = dialog.getListView();

        return dialog;
    }

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

    @Subscribe
    public void handleTheme(Theme theme) {
        mIsInternalsVisible = theme.isInternalsVisible;
        loadConnectionsList();
    }

    @Subscribe
    public void handlePeerList(PeerListProducer.PeerList peerList) {
        for (Node.Peer peer : peerList) {
            if (peer.id == mPeerId) {
                mPeer = peer;
                loadConnectionsList();
                break;
            }
        }
    }

    /**
     * Loads the list of outgoing connections.
     */
    private void loadConnectionsList() {
        if (mListView != null && mPeer != null && mIsInternalsVisible != null) {
            final Credential[] outgoingConnections = mPeer.getOutgoingConnections();
            if (outgoingConnections != null && outgoingConnections.length > 0) {
                final ListAdapter adapter = new ConnectionAdapter(getActivity(), mBus, mPeer, mIsInternalsVisible);
                mListView.setAdapter(adapter);
                adapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        if (adapter.getCount() <= 0) {
                            dismiss();
                        }
                    }
                });
            } else {
                dismiss();
            }
        }
    }

    public static DialogFragment newInstance(int peerId) {
        DialogFragment fragment = new ConnectionsDialogFragment();

        Bundle args = new Bundle();
        args.putInt(FRAGMENT_BUNDLE_KEY_PEER_ID, peerId);
        fragment.setArguments(args);

        return fragment;
    }

    private static class ConnectionAdapter extends BaseAdapter {

        private Bus mBus;

        private Resources mRes;

        private LayoutInflater mInflater;

        private Node.Peer mPeer;

        private boolean mIsInternalsVisible;

        private ConnectionAdapter(Context context, Bus bus, Node.Peer peer, boolean isInternalsVisible) {
            mRes = context.getResources();
            mInflater = LayoutInflater.from(context);
            mBus = bus;
            mPeer = peer;
            mIsInternalsVisible = isInternalsVisible;
        }

        @Override
        public int getCount() {
            if (mPeer == null || mPeer.getOutgoingConnections() == null) {
                return 0;
            }
            return mPeer.getOutgoingConnections().length;
        }

        @Override
        public Credential getItem(int position) {
            if (mPeer == null || mPeer.getOutgoingConnections() == null) {
                return null;
            }
            return mPeer.getOutgoingConnections()[position];
        }

        @Override
        public long getItemId(int position) {
            if (mPeer == null || mPeer.getOutgoingConnections() == null) {
                return -1L;
            }
            return mPeer.getOutgoingConnections()[position].id;
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
            holder.protocol.setText(mRes.getString(R.string.connections_list_item_protocol, credential.protocol.getDescription(mRes)));
            if (mIsInternalsVisible) {
                holder.password.setText(mRes.getString(R.string.connections_list_item_password, credential.password));
                holder.password.setVisibility(View.VISIBLE);
            } else {
                holder.password.setVisibility(View.GONE);
            }
            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Credential> connections = new ArrayList<>(Arrays.asList(mPeer.getOutgoingConnections()));
                    connections.remove(credential);
                    mPeer.setOutgoingConnections(connections.toArray(new Credential[connections.size()]));
                    notifyDataSetChanged();

                    mBus.post(new PeerEvents.Update(mPeer));
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
