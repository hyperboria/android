package berlin.meshnet.cjdns;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        writeCjdroute();
    }

    private boolean writeCjdroute()
    {
        File target = new File(getCacheDir(), "cjdroute");
        if (target.exists() && target.canExecute()) {
            Log.i("cjdns_MainActivity", "cjdroute exists and is executable");
            return true;
        }

        try {
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
                return false;
            }
        } catch (IOException e) {
            Log.e("cjdns_MainActivity", "IOException: " + e.getMessage());
            return false;
        }

        Log.i("cjdns_MainActivity", "Created cjdroute and made it executable");
        return true;
    }
}
