package io.remonode.notifytest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
 *
 * Under that header sits a fake sign-in with a real second factor, for remonode's "Login still works"
 * template: email + password, then a 6-digit authenticator code, then a signed-in screen to assert on. It is
 * on this screen rather than its own activity because Launch App opens the launcher activity, and a second
 * launcher entry would make which one opens a coin toss. The header stays, so every workflow that waits for
 * CANARY OK keeps passing.
 *
 * The flow is shaped by how remonode's Input Text types, not by how a person would:
 *
 *  - A field picked by selector is filled with setValue, which does NOT reliably move focus to it. So Enter is
 *    handled for the whole screen (dispatchKeyEvent), not per field: "Press Enter after typing" on the password
 *    step signs in wherever focus happens to be.
 *  - The code is typed into "whatever has focus right now" — blind key events. So the code screen focuses its
 *    field the moment it appears, and any key that still arrives elsewhere is routed to it.
 */
public class MainActivity extends Activity {

    // The test account. Public on purpose — this is a canary app on test devices, and the README lists them.
    // The email matches the template's Set Values default, so only the password and the key need entering.
    static final String TEST_EMAIL = "qa@example.com";
    static final String TEST_PASSWORD = "canary-password";
    /** Base32 setup key for the authenticator / remonode's 2FA Code (TOTP) node. SHA1, 6 digits, 30 s. */
    static final String TOTP_SECRET = "REMONODECANARY23";

    private enum Step { SIGN_IN, CODE, SIGNED_IN }

    private Step step = Step.SIGN_IN;
    private LinearLayout card;
    private TextView error;
    private EditText email;
    private EditText password;
    private EditText code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));

        root.addView(line("CANARY OK", 34, "#FF6D5A"));
        root.addView(line("build " + versionName(), 18, "#ECECF1"));
        root.addView(line(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()),
                16, "#A2A2B2"));

        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#16161F"));
        bg.setStroke(dp(1), Color.parseColor("#2A2A3A"));
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(28);
        root.addView(card, cardLp);

        // For the "Crash & ANR watch" template, whose log check can only take its crash branch if something
        // crashes: one Tap on either of these (id=io.remonode.notifytest:id/crash or …/freeze) and the log it
        // is recording has a FATAL EXCEPTION or an ANR in it. Outside the card so they are there at every step.
        TextView stability = line("Stability test", 14, "#6E6E80");
        stability.setPadding(0, dp(28), 0, 0);
        root.addView(stability);
        root.addView(button(R.id.crash, "Crash now", v -> {
            throw new IllegalStateException("Deliberate crash from the Crash now button");
        }));
        root.addView(button(R.id.freeze, "Freeze (ANR)", v -> FreezeReceiver.freeze(this)));

        // Scrolls so the card stays reachable above the soft keyboard on a small phone.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.parseColor("#0D0D17"));
        scroll.addView(root);
        setContentView(scroll);

        showSignIn();
    }

    // Leaving the app ends the session. Launch App resumes a running app where it was, and a canary that
    // resumed already signed in would skip the very screens the login test exists to exercise.
    @Override
    protected void onStop() {
        super.onStop();
        if (step != Step.SIGN_IN) showSignIn();
    }

    // ── Screens ────────────────────────────────────────────────────────────────────────────────────────

    private void showSignIn() {
        step = Step.SIGN_IN;
        card.removeAllViews();
        header("Sign in");
        email = field(R.id.email, "Email", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                EditorInfo.IME_ACTION_NEXT);
        password = field(R.id.password, "Password",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, EditorInfo.IME_ACTION_DONE);
        error = errorLine();
        card.addView(button(R.id.sign_in, "Sign in", v -> submitPassword()));
        card.addView(hint("Test account: " + TEST_EMAIL + " · " + TEST_PASSWORD));
        code = null;
    }

    private void showCode() {
        step = Step.CODE;
        card.removeAllViews();
        header("Two-step verification");
        card.addView(body("Enter the 6-digit code from your authenticator app."));
        code = field(R.id.code, "6-digit code", InputType.TYPE_CLASS_NUMBER, EditorInfo.IME_ACTION_DONE);
        // Checked as soon as the sixth digit lands, so a person typing by hand needs no extra tap. Enter still
        // works (and is what the template sends); a second check of the same code just repeats the answer.
        code.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 6) submitCode();
            }
        });
        error = errorLine();
        card.addView(button(R.id.verify, "Verify", v -> submitCode()));
        card.addView(hint("Setup key: " + TOTP_SECRET));
        card.addView(button(View.NO_ID, "Back", v -> showSignIn()));
        // The workflow types the code into whatever has focus, straight after the password step — so the
        // field must already have it. Posted so it lands after this layout pass, not before the view exists.
        code.post(this::focusCode);
    }

    private void showSignedIn() {
        step = Step.SIGNED_IN;
        hideKeyboard();
        card.removeAllViews();
        TextView ok = line("SIGNED IN", 28, "#4ADE80");
        ok.setId(R.id.signed_in);
        card.addView(ok);
        TextView who = line("as " + TEST_EMAIL, 16, "#ECECF1");
        who.setId(R.id.account);
        card.addView(who);
        card.addView(line(
                "at " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()), 14, "#A2A2B2"));
        card.addView(button(R.id.sign_out, "Sign out", v -> showSignIn()));
        error = null;
        code = null;
    }

    // ── Actions ────────────────────────────────────────────────────────────────────────────────────────

    private void submitPassword() {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();
        if (e.isEmpty() || p.isEmpty()) {
            showError("Enter your email and password.");
        } else if (!e.equalsIgnoreCase(TEST_EMAIL) || !p.equals(TEST_PASSWORD)) {
            // One message for both, as a real login would — but the canary names which field was wrong in the
            // log, because a run that fails here is usually a typo in the workflow, not a broken app.
            showError("Wrong email or password.");
            Log.w("Canary", "sign-in rejected: email " + (e.equalsIgnoreCase(TEST_EMAIL) ? "ok" : "wrong")
                    + ", password " + (p.equals(TEST_PASSWORD) ? "ok" : "wrong"));
        } else {
            showCode();
        }
    }

    private void submitCode() {
        if (code == null) return;
        String c = code.getText().toString().replaceAll("\\s", "");
        // The template sends Enter after the sixth digit, which the auto-check has already answered. An empty
        // field here is that Enter arriving after a rejected code was cleared — keep the rejection on screen.
        if (c.isEmpty() && error != null && error.getVisibility() == View.VISIBLE) return;
        if (Totp.verify(TOTP_SECRET, c, System.currentTimeMillis())) {
            showSignedIn();
        } else {
            showError(c.length() < 6 ? "Enter all 6 digits." : "That code didn't work. Try the current one.");
            code.setText("");
            focusCode();
        }
    }

    // Enter from anywhere on the screen is the screen's submit — see the class comment for why it can't be a
    // per-field listener. Consumed here, so a focused single-line field doesn't also fire its editor action.
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int k = event.getKeyCode();
        if (k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (step == Step.SIGN_IN) submitPassword();
                else if (step == Step.CODE) submitCode();
            }
            return true;
        }
        // Blind typing on the code screen goes into the code field even if something stole focus.
        if (step == Step.CODE && code != null && !code.hasFocus()) code.requestFocus();
        return super.dispatchKeyEvent(event);
    }

    private void showError(String msg) {
        if (error == null) return;
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }

    private void focusCode() {
        if (code == null) return;
        code.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(code, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (f != null && imm != null) imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
    }

    // ── Views ──────────────────────────────────────────────────────────────────────────────────────────

    private void header(String text) {
        TextView tv = new TextView(this);
        tv.setId(R.id.step);
        tv.setText(text);
        tv.setTextSize(20);
        tv.setTextColor(Color.parseColor("#ECECF1"));
        tv.setPadding(0, 0, 0, dp(12));
        card.addView(tv);
    }

    private TextView body(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(Color.parseColor("#A2A2B2"));
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    private TextView hint(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(Color.parseColor("#6E6E80"));
        tv.setPadding(0, dp(12), 0, 0);
        return tv;
    }

    private TextView errorLine() {
        TextView tv = new TextView(this);
        tv.setId(R.id.error);
        tv.setTextSize(14);
        tv.setTextColor(Color.parseColor("#F87171"));
        tv.setPadding(0, dp(4), 0, dp(4));
        tv.setVisibility(View.GONE);
        card.addView(tv);
        return tv;
    }

    private EditText field(int id, String hintText, int inputType, int imeAction) {
        EditText et = new EditText(this);
        et.setId(id);
        et.setHint(hintText);
        et.setInputType(inputType);
        et.setImeOptions(imeAction);
        et.setSingleLine(true);
        et.setTextSize(17);
        et.setTextColor(Color.parseColor("#ECECF1"));
        et.setHintTextColor(Color.parseColor("#6E6E80"));
        // The soft keyboard's Done/Go button arrives as an editor action, not a key event.
        et.setOnEditorActionListener((v, action, ev) -> {
            if (action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO) {
                if (step == Step.SIGN_IN) submitPassword();
                else if (step == Step.CODE) submitCode();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.addView(et, lp);
        return et;
    }

    private Button button(int id, String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
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
