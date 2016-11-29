package berlin.meshnet.cjdns.dagger;

import javax.inject.Singleton;

import berlin.meshnet.cjdns.CjdnsService;
import berlin.meshnet.cjdns.CjdnsVpnService;
import berlin.meshnet.cjdns.MainActivity;
import berlin.meshnet.cjdns.dialog.ConnectionsDialogFragment;
import berlin.meshnet.cjdns.page.AboutPageFragment;
import berlin.meshnet.cjdns.page.CredentialsPageFragment;
import berlin.meshnet.cjdns.page.MePageFragment;
import berlin.meshnet.cjdns.page.PeersPageFragment;
import berlin.meshnet.cjdns.page.SettingsPageFragment;
import dagger.Component;

/**
 * {@link Component} providing core dependencies.
 */
@Singleton
@Component(modules = DefaultModule.class)
public interface CoreComponent {

    void inject(MainActivity inject);

    void inject(CjdnsVpnService inject);

    void inject(CjdnsService inject);

    void inject(MePageFragment inject);

    void inject(PeersPageFragment inject);

    void inject(CredentialsPageFragment inject);

    void inject(SettingsPageFragment inject);

    void inject(AboutPageFragment inject);

    void inject(ConnectionsDialogFragment inject);
}
