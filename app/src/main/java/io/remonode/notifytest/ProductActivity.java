package io.remonode.notifytest;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Where a deep link lands, for remonode's "Deep link check" template. That template opens
 * https://example.com/product/123 and asserts it arrived on the right screen instead of a web page, so this
 * screen answers to exactly that link, and to remonode-canary://product/123 as well:
 *
 *  - With Open URL's Target app set to io.remonode.notifytest, the https link is forced into this app and
 *    lands here — the passing run.
 *  - With Target app empty, nothing has VERIFIED example.com (there is no autoVerify, and there could not be:
 *    we don't own the domain), so Android hands it to the browser — which is precisely the failure the
 *    template exists to catch, reproduced on purpose. The custom scheme has no browser to fall back to, so it
 *    lands here either way.
 *
 * The product id is read from the link rather than fixed, so a workflow can prove the link's DATA arrived and
 * not just that some screen of the app opened: /product/42 shows PRODUCT 42.
 */
public class ProductActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri link = getIntent().getData();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#0D0D17"));
        root.setPadding(48, 48, 48, 48);

        root.addView(line("CANARY OK", 22, "#FF6D5A", 0));
        TextView title = line("PRODUCT " + productId(link), 34, "#ECECF1", R.id.product);
        root.addView(title);
        root.addView(line("opened from " + (link == null ? "(no link)" : link.toString()), 14, "#A2A2B2",
                R.id.deep_link));

        setContentView(root);
    }

    /** The segment after "product": https://host/product/123 and remonode-canary://product/123 both give 123. */
    static String productId(Uri link) {
        if (link == null) return "?";
        // For the custom scheme "product" is the HOST (remonode-canary://product/123), so the id is the first
        // path segment; for https it is a path segment itself, and the id follows it.
        if ("product".equals(link.getHost())) {
            List<String> seg = link.getPathSegments();
            return seg.isEmpty() ? "?" : seg.get(0);
        }
        List<String> seg = link.getPathSegments();
        int i = seg.indexOf("product");
        return i >= 0 && i + 1 < seg.size() ? seg.get(i + 1) : "?";
    }

    private TextView line(String text, int sizeSp, String color, int id) {
        TextView tv = new TextView(this);
        if (id != 0) tv.setId(id);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(Color.parseColor(color));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 12, 0, 12);
        return tv;
    }
}
