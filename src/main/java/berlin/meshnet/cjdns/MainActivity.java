package berlin.meshnet.cjdns;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
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

import javax.inject.Inject;

import berlin.meshnet.cjdns.dialog.ExchangeDialogFragment;
import berlin.meshnet.cjdns.event.ExchangeEvent;
import berlin.meshnet.cjdns.event.PageChangeEvent;
import berlin.meshnet.cjdns.event.StartCjdnsServiceEvent;
import berlin.meshnet.cjdns.event.StopCjdnsServiceEvent;
import berlin.meshnet.cjdns.page.AboutPageFragment;
import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.page.SettingsPageFragment;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends ActionBarActivity {

    private static final String BUNDLE_KEY_SELECTED_CONTENT = "selectedContent";

    @Inject
    Bus mBus;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @InjectView(R.id.drawer)
    ListView mDrawer;

    private SwitchCompat mCjdnsServiceSwitch;

    private ActionBarDrawerToggle mDrawerToggle;

    private ArrayAdapter<String> mDrawerAdapter;

    private String mSelectedContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inject dependencies.
        ((CjdnsApplication) getApplication()).inject(this);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);

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
        mDrawer.setItemChecked(1, true); // TODO Create state producer
        mDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDrawerLayout.closeDrawer(Gravity.START);

                final String selectedOption = mDrawerAdapter.getItem(position);
                if (!selectedOption.equals(mSelectedContent)) {
                    mBus.post(new PageChangeEvent(selectedOption));
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open_content_desc, R.string.drawer_close_content_desc) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mToolbar.setTitle(mSelectedContent);
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mToolbar.setTitle(getString(R.string.app_name));
                supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();

        // Show Me page on first launch.
        if (savedInstanceState == null) {
            changePage(getString(R.string.drawer_option_me));
        } else {
            final String selectedPage = savedInstanceState.getString(BUNDLE_KEY_SELECTED_CONTENT, getString(R.string.drawer_option_me));
            mSelectedContent = selectedPage;
            mToolbar.setTitle(selectedPage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        mCjdnsServiceSwitch = (SwitchCompat) menu.findItem(R.id.switch_cjdns_service).getActionView();
        // TODO Init with current CjdnsService state
        mCjdnsServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mBus.post(new StartCjdnsServiceEvent());
                } else {
                    mBus.post(new StopCjdnsServiceEvent());
                }
            }
        });
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

    @Subscribe
    public void handleEvent(StartCjdnsServiceEvent event) {
        Toast.makeText(getApplicationContext(), "Starting CjdnsService", Toast.LENGTH_SHORT).show();
        startService(new Intent(getApplicationContext(), CjdnsService.class));
    }

    @Subscribe
    public void handleEvent(StopCjdnsServiceEvent event) {
        Toast.makeText(getApplicationContext(), "Stopping CjdnsService", Toast.LENGTH_SHORT).show();
        stopService(new Intent(getApplicationContext(), CjdnsService.class));
    }

    @Subscribe
    public void handleEvent(PageChangeEvent event) {
        changePage(event.mSelectedContent);
    }

    @Subscribe
    public void handleEvent(ExchangeEvent event) {
        DialogFragment fragment = ExchangeDialogFragment.newInstance(event.mType, event.mMessage);
        fragment.show(getFragmentManager(), null);
    }

    /**
     * Change page to selection.
     *
     * @param selectedPage The selected page.
     */
    private void changePage(final String selectedPage) {
        mSelectedContent = selectedPage;
        mToolbar.setTitle(selectedPage);

        Fragment fragment = null;
        if (getString(R.string.drawer_option_me).equals(selectedPage)) {
            fragment = MePageFragment.newInstance();
        } else if (getString(R.string.drawer_option_credentials).equals(selectedPage)) {
            fragment = CredentialsPageFragment.newInstance();
        } else if (getString(R.string.drawer_option_settings).equals(selectedPage)) {
            fragment = SettingsPageFragment.newInstance();
        } else if (getString(R.string.drawer_option_about).equals(selectedPage)) {
            fragment = AboutPageFragment.newInstance();
        }

        // Swap page.
        if (fragment != null) {
            final FragmentManager fragmentManager = getFragmentManager();
            final FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.content_container, fragment);
            ft.commit();
        }
    }
}
