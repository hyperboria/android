package berlin.meshnet.cjdns.event;

import berlin.meshnet.cjdns.model.Credential;

public class AuthorizedCredentialEvents {

    public static class Create {
    }

    public static class Update {

        public final Credential.Authorized mCredential;

        public Update(Credential.Authorized credential) {
            mCredential = credential;
        }
    }

    public static class Remove {

        public final int mId;

        public Remove(int id) {
            mId = id;
        }
    }

    public static class New {

        public final Credential.Authorized mCredential;

        public New(Credential.Authorized credential) {
            mCredential = credential;
        }
    }
}
