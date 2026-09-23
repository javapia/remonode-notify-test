package io.remonode.notifytest;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One screen, built in code — no layout XML, no AppCompat, no Compose. Everything here exists to be
 * ASSERTED ON by a remonode workflow running this app unattended, so the rules are:
 *
 *  - CANARY OK is a fixed string that appears only when onCreate finished. It is what "Wait for Element"
 *    matches on, and it is deliberately not the app's name or label: a launcher icon and a title bar are on
 *    screen before the app has done anything, so matching those would pass for an app that drew nothing.
 *  - The version line names the CI build the APK came from, so the screenshot mailed back on success proves
 *    WHICH build was tested — the whole point of pulling the release permalink every morning is lost if the
 *    evidence can't tell yesterday's APK from today's.
 *  - The launch time is local to the device, and is there to prove the screen is this run's and not a
 *    cached screenshot.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0D0D17"));
        root.setPadding(48, 48, 48, 48);

        root.addView(line("CANARY OK", 34, "#FF6D5A"));
        root.addView(line("build " + versionName(), 18, "#ECECF1"));
        root.addView(line(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()),
                16, "#A2A2B2"));

        setContentView(root);
    }

    private TextView line(String text, int sizeSp, String color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(Color.parseColor(color));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 12, 0, 12);
        return tv;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // Cannot happen — this is our own package — but an exception here must not be what a crash-on-
            // launch test sees, because then the canary would be testing this method instead of the app.
            return "unknown";
        }
    }
}
