package berlin.meshnet.cjdns.page;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import berlin.meshnet.cjdns.CjdnsApplication;
import berlin.meshnet.cjdns.R;
import berlin.meshnet.cjdns.model.Credential;
import berlin.meshnet.cjdns.model.Theme;
import berlin.meshnet.cjdns.producer.CredentialListProducer;
import berlin.meshnet.cjdns.producer.ThemeProducer;
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

    private RecyclerView.Adapter mAdapter;

    public static final Fragment newInstance() {
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
    }

    @Subscribe
    public void handleCredentialList(CredentialListProducer.CredentialList credentialList) {
        mAdapter = new CredentialListAdapter(getActivity(), credentialList);
        mCredentialsRecyclerView.setAdapter(mAdapter);
    }

    static class CredentialListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private static final float ALPHA_ALLOWED = 1f;

        private static final float ALPHA_REVOKED = 0.3f;

        private Resources mResources;

        private CredentialListProducer.CredentialList mCredentialList;

        private CredentialListAdapter(Context context, CredentialListProducer.CredentialList credentialList) {
            mResources = context.getApplicationContext().getResources();
            mCredentialList = credentialList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_credential, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Credential credential = mCredentialList.get(position);
            holder.label.setText(credential.label);
            holder.protocol.setText(credential.protocol.getDescription(mResources));
            holder.password.setText(credential.password);
            holder.allow.setText(credential.isAllowed()
                    ? mResources.getString(R.string.credential_card_allow_button_on)
                    : mResources.getString(R.string.credential_card_allow_button_off));
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

        @InjectView(R.id.credential_card_password)
        TextView password;

        @InjectView(R.id.credential_card_allow)
        IconTextView allow;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
    }
}
