package io.remonode.notifytest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A screen that is broken on purpose, for remonode's "AI screen check" template. Its prompt asks the model to
 * flag "error messages, empty states, missing content or anything that looks broken", and the canary's normal
 * screen has none of those — so the template could only ever be seen saying "looks fine". Opened with
 * remonode-canary://broken, this screen has one of each, the way they actually show up in a real app:
 *
 *  - an error banner with a status code,
 *  - a list whose header says it has items and whose body says it has none,
 *  - a greeting whose template variable never got filled in,
 *  - a price that rendered as "null".
 *
 * Nothing here crashes: the app is running and the screen is drawn, which is precisely the kind of broken
 * that "is the app running?" checks pass and a person looking at the screenshot would not.
 */
public class BrokenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D0D17"));
        root.setPadding(48, 48, 48, 48);

        TextView banner = line("Couldn't load your orders (HTTP 500). Try again later.", 16, "#FFFFFF");
        banner.setId(R.id.broken);
        banner.setBackgroundColor(Color.parseColor("#B91C1C"));
        banner.setPadding(32, 24, 32, 24);
        root.addView(banner);

        root.addView(line("Hi, {{user.first_name}}!", 26, "#ECECF1"));
        root.addView(line("Your orders (3)", 18, "#ECECF1"));
        root.addView(line("No items to show.", 16, "#6E6E80"));
        root.addView(line("Order total: $null", 18, "#ECECF1"));

        setContentView(root);
    }

    private TextView line(String text, int sizeSp, String color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(Color.parseColor(color));
        tv.setGravity(Gravity.START);
        tv.setPadding(0, 24, 0, 24);
        return tv;
    }
}
