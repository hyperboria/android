package berlin.meshnet.cjdns.event;

import berlin.meshnet.cjdns.model.Credential;

public class CredentialEvents {

    public static class Create {
    }

    public static class Update {

        public final Credential mCredential;

        public Update(Credential credential) {
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

        public final Credential mCredential;

        public New(Credential credential) {
            mCredential = credential;
        }
    }
}
