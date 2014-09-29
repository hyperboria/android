package berlin.meshnet.cjdns;

import android.util.Log;
import android.widget.TextView;

import java.io.File;

public class CjdnsThread implements Runnable {
    private File cjdroute;
    private TextView logView;

    public CjdnsThread(File cjdroute, TextView logView)
    {
        this.cjdroute = cjdroute;
        this.logView = logView;
    }

    @Override
    public void run()
    {
        logView.setText("CjdnsThread: " + cjdroute.getPath());
    }
}
