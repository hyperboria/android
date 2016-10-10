package berlin.meshnet.cjdns;

import java.io.IOException;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;

/**
 * Utility for sending a file descriptor through a named pipe.
 */
public class FileDescriptorSender {

    static {
        System.loadLibrary("sendfd");
    }

    /**
     * Native method for sending file descriptor through the named pipe.
     *
     * @param path            The path to the named pipe.
     * @param file_descriptor The file descriptor.
     * @return {@code 0} if successful; {@code -1} if failed.
     */
    public static native int sendfd(String path, int file_descriptor);

    /**
     * Sends a file descriptor through the named pipe.
     *
     * @param path The path to the named pipe.
     * @param fd   The file descriptor.
     * @return {@link Observable} that emits {@code true} if successful; otherwise
     * {@link Subscriber#onError(Throwable)} is called.
     */
    static Observable<Boolean> send(final String path, final int fd) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    if (FileDescriptorSender.sendfd(path, fd) == 0) {
                        subscriber.onNext(true);
                        subscriber.onCompleted();
                    } else {
                        Exception e = new IOException(String.format(Locale.ENGLISH,
                                "Failed to send file descriptor %1$s to named pipe %2$s", fd, path));
                        subscriber.onError(e);
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
