package berlin.meshnet.cjdns.event;

import berlin.meshnet.cjdns.model.Credential;

/**
 * Events associated with {@link berlin.meshnet.cjdns.model.Credential.Authorized}.
 */
public interface AuthorizedCredentialEvents {

    /**
     * Request to create a new {@link berlin.meshnet.cjdns.model.Credential.Authorized}.
     */
    static class Create {
    }

    /**
     * Request to update a {@link berlin.meshnet.cjdns.model.Credential.Authorized}.
     */
    static class Update {

        public final Credential.Authorized mCredential;

        public Update(Credential.Authorized credential) {
            mCredential = credential;
        }
    }

    /**
     * Request to remove a {@link berlin.meshnet.cjdns.model.Credential.Authorized}.
     */
    static class Remove {

        public final Credential.Authorized mCredential;

        public Remove(Credential.Authorized credential) {
            mCredential = credential;
        }
    }
}
