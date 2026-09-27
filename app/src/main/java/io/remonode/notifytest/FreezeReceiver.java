package io.remonode.notifytest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * A deliberate ANR, for remonode's "Crash & ANR watch" template.
 *
 * It has to be an ANR Android declares with nobody touching the phone, because an unattended run has nothing
 * to send the app next. Blocking the main thread in a click handler only becomes an ANR once a SECOND input
 * event is waiting, and a blocked service's start timeout did not fire at all on API 35. A foreground broadcast
 * does: its receiver gets 10 s, then ActivityManager logs "ANR in io.remonode.notifytest" by itself.
 */
public class FreezeReceiver extends BroadcastReceiver {

    static final long FREEZE_MS = 30_000;

    static void freeze(Context context) {
        Intent i = new Intent(context, FreezeReceiver.class);
        // Foreground: the 10 s timeout. A background broadcast gets 60 s, longer than the run should wait.
        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(i);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("Canary", "freezing the main thread for " + FREEZE_MS + " ms (deliberate ANR)");
        SystemClock.sleep(FREEZE_MS);
    }
}
