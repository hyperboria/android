package berlin.meshnet.cjdns;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import berlin.meshnet.cjdns.event.PageChangeEvent;
import berlin.meshnet.cjdns.event.StartCjdnsServiceEvent;
import berlin.meshnet.cjdns.event.StopCjdnsServiceEvent;
import berlin.meshnet.cjdns.page.MePageFragment;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends ActionBarActivity {

    @Inject
    Bus mBus;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    @InjectView(R.id.drawer)
    ListView mDrawer;

    @InjectView(R.id.content_container)
    FrameLayout mContentContainer;

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
                    mSelectedContent = selectedOption;
                    mBus.post(new PageChangeEvent(selectedOption));
                }
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open_content_desc, R.string.drawer_close_content_desc) {
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                mToolbar.setTitle(mDrawerAdapter.getItem(mDrawer.getCheckedItemPosition()));
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
        // TODO Stop CjdnsService
    }

    @Subscribe
    public void handleEvent(PageChangeEvent event) {
        Toast.makeText(getApplicationContext(), "Changing content", Toast.LENGTH_SHORT).show();

        // Get page corresponding to selection.
        Fragment fragment = null;
        if (getString(R.string.drawer_option_me).equals(event.mSelectedContent)) {
            fragment = MePageFragment.newInstance();
        }

        // Swap page.
        if (fragment != null) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            final FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.content_container, fragment);
            ft.commit();
        }
    }
}
