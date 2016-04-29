package berlin.meshnet.cjdns;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import berlin.meshnet.cjdns.dialog.ConnectionsDialogFragment;
import berlin.meshnet.cjdns.dialog.ExchangeDialogFragment;
import berlin.meshnet.cjdns.event.ApplicationEvents;
import berlin.meshnet.cjdns.page.AboutPageFragment;
import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.page.PeersPageFragment;
import berlin.meshnet.cjdns.page.SettingsPageFragment;
import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String BUNDLE_KEY_SELECTED_CONTENT = "selectedContent";

    @Inject
    Bus mBus;

    @Inject
    Cjdroute mCjdroute;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @InjectView(R.id.drawer)
    ListView mDrawer;

    private SwitchCompat mSwitch;

    private ActionBarDrawerToggle mDrawerToggle;

    private ArrayAdapter<String> mDrawerAdapter;

    private String mSelectedContent;

    private ActionBar mActionBar;

    private List<Subscription> mSubscriptions = new ArrayList<>();

    private boolean mIsCjdnsRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();

        // Inject dependencies.
        ((CjdnsApplication) getApplication()).inject(this);
        ButterKnife.inject(this);

        final TypedArray drawerIcons = getResources().obtainTypedArray(R.array.drawer_icons);
        mDrawerAdapter = new ArrayAdapter<String>(this, R.layout.view_drawer_option, getResources().getStringArray(R.array.drawer_options)) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setCompoundDrawablesWithIntrinsicBounds(null, null, drawerIcons.getDrawable(position), null);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setCompoundDrawablesWithIntrinsicBounds(null, null, drawerIcons.getDrawable(position), null);
                return view;
            }
        };
        mDrawer.setAdapter(mDrawerAdapter);
        mDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(GravityCompat.START);

                final String selectedOption = mDrawerAdapter.getItem(position);
                if (!selectedOption.equals(mSelectedContent)) {
                    mBus.post(new ApplicationEvents.ChangePage(selectedOption));
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open_content_desc, R.string.drawer_close_content_desc) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mActionBar.setTitle(mSelectedContent);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mActionBar.setTitle(getString(R.string.app_name));
                supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();

        // Show Me page on first launch.
        if (savedInstanceState == null || !savedInstanceState.containsKey(BUNDLE_KEY_SELECTED_CONTENT)) {
            changePage(getString(R.string.drawer_option_me));
        } else {
            final String selectedPage = savedInstanceState.getString(BUNDLE_KEY_SELECTED_CONTENT);
            mSelectedContent = selectedPage;
            mActionBar.setTitle(selectedPage);
        }

        // Select corresponding drawer option.
        String[] options = getResources().getStringArray(R.array.drawer_options);
        for (int index = 0; index < options.length; index++) {
            if (options[index].equals(mSelectedContent)) {
                mDrawer.setItemChecked(index, true);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // TODO Sync toggle properly.

        // Configure toggle click behaviour.
        mSwitch = (SwitchCompat) MenuItemCompat.getActionView(menu.findItem(R.id.switch_cjdns_service));
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !mIsCjdnsRunning) {
                    mBus.post(new ApplicationEvents.StartCjdnsService());
                    mIsCjdnsRunning = true;
                } else if (!isChecked && mIsCjdnsRunning) {
                    mBus.post(new ApplicationEvents.StopCjdnsService());
                    mIsCjdnsRunning = false;
                }
            }
        });

        // Set initial state of toggle and click behaviour.
        try {
            mSubscriptions.add(AppObservable.bindActivity(this, Cjdroute.running()
                    .subscribeOn(Schedulers.io()))
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long pid) {
                            // Change toggle check state if there is a currently running cjdroute process.
                            mSwitch.setChecked(mIsCjdnsRunning = pid != Cjdroute.INVALID_PID);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            mSwitch.setChecked(mIsCjdnsRunning = false);
                        }
                    }));
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to start AdminApi", e);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBus.register(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(BUNDLE_KEY_SELECTED_CONTENT, mSelectedContent);
    }

    @Override
    public void onPause() {
        mBus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Unsubscribe from observables.
        Iterator<Subscription> itr = mSubscriptions.iterator();
        while (itr.hasNext()) {
            itr.next().unsubscribe();
            itr.remove();
        }

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Subscribe
    public void handleEvent(ApplicationEvents.StartCjdnsService event) {
        Toast.makeText(getApplicationContext(), "Starting CjdnsService", Toast.LENGTH_SHORT).show();

        // Start cjdns VPN.
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }

        // TODO Compat.
//        startService(new Intent(getApplicationContext(), CjdnsService.class));
    }


    @Subscribe
    public void handleEvent(ApplicationEvents.StopCjdnsService event) {
        Toast.makeText(getApplicationContext(), "Stopping CjdnsService", Toast.LENGTH_SHORT).show();

        // Kill cjdroute process.
        try {
            Cjdroute.running()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(mCjdroute.terminate());
        } catch (UnknownHostException e) {
            Log.e(TAG, "Failed to start AdminApi", e);
        }

        // TODO Do this properly.
        mSwitch.setChecked(false);

        // TODO Compat.
//        stopService(new Intent(getApplicationContext(), CjdnsService.class));
    }

    @Subscribe
    public void handleEvent(ApplicationEvents.ChangePage event) {
        changePage(event.mSelectedContent);
    }

    @Subscribe
    public void handleEvent(ApplicationEvents.ListConnections event) {
        DialogFragment fragment = ConnectionsDialogFragment.newInstance(event.mPeerId);
        fragment.show(getSupportFragmentManager(), null);
    }

    @Subscribe
    public void handleEvent(ApplicationEvents.ExchangeCredential event) {
        DialogFragment fragment = ExchangeDialogFragment.newInstance(event.mType, event.mMessage);
        fragment.show(getSupportFragmentManager(), null);
    }

    /**
     * Change page to selection.
     *
     * @param selectedPage The selected page.
     */
    private void changePage(final String selectedPage) {
        mSelectedContent = selectedPage;
        mActionBar.setTitle(selectedPage);

        Fragment fragment = null;
        if (getString(R.string.drawer_option_me).equals(selectedPage)) {
            fragment = MePageFragment.newInstance();
        } else if (getString(R.string.drawer_option_peers).equals(selectedPage)) {
            fragment = PeersPageFragment.newInstance();
        } else if (getString(R.string.drawer_option_credentials).equals(selectedPage)) {
            fragment = CredentialsPageFragment.newInstance();
        } else if (getString(R.string.drawer_option_settings).equals(selectedPage)) {
            fragment = SettingsPageFragment.newInstance();
        } else if (getString(R.string.drawer_option_about).equals(selectedPage)) {
            fragment = AboutPageFragment.newInstance();
        }

        // Swap page.
        if (fragment != null) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            final FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.content_container, fragment);
            ft.commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(new Intent(this, CjdnsVpnService.class));
        }
    }
}
