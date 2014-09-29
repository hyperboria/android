package berlin.meshnet.cjdns;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        try {
            writeCjdroute();
            startCjdnsThread((TextView)findViewById(R.id.hello));
        } catch (IOException e) {
            Log.e("cjdns_MainActivity", "IOException: " + e.getMessage());
        }
    }

    private void startCjdnsThread(TextView logView) throws IOException
    {
        Thread cjdns = new Thread(new CjdnsThread(cjdroute(), cjdrouteconf(), logView), "CjdnsThread");
        cjdns.start();
    }

    private void writeCjdroute() throws IOException
    {
        File target = cjdroute();
        if (target.exists() && target.canExecute()) {
            Log.i("cjdns_MainActivity", "cjdroute exists and is executable");
            return;
        }

        InputStream asset = getAssets().open("cjdroute");
        FileOutputStream cjdroute = new FileOutputStream(target);
        byte buf[] = new byte[4096];

        int len = asset.read(buf);
        while (len > 0) {
            cjdroute.write(buf, 0, len);
            len = asset.read(buf);
        }
        cjdroute.close();

        if (!target.setExecutable(true)) {
            Log.e("cjdns_MainActivity", "setExecutable() failed");
            return;
        }

        Log.i("cjdns_MainActivity", "Created cjdroute and made it executable");
    }

    private File cjdroute()
    {
        return new File(getCacheDir(), "cjdroute");
    }

    private InputStream cjdrouteconf() throws IOException
    {
        return getAssets().open("cjdroute.conf");
    }
}
