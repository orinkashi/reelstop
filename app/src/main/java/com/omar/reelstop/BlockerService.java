package com.omar.reelstop;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Kaychouf ila l'utilisateur dkhel l Reels (Instagram), Shorts (YouTube) wla TikTok,
 * w kayrjj3o.
 *
 * Detection katkhdem b "view IDs" dyal les apps. Ila Instagram/YouTube bdlo l IDs
 * f chi mise à jour, khass nzido les IDs jdad f les listes lta7t.
 */
public class BlockerService extends AccessibilityService {

    static final String PREFS = "reelstop";
    static final String KEY_IG = "instagram";
    static final String KEY_YT = "youtube";
    static final String KEY_TT = "tiktok";
    static final String KEY_IG_FEED = "instagram_feed";
    static final String KEY_COUNT = "count";

    private static final String IG = "com.instagram.android";
    private static final String YT = "com.google.android.youtube";
    private static final String[] TIKTOK = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};

    // IDs dyal l'écran Reels f Instagram
    private static final String[] IG_REELS_IDS = {
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "root_clips_layout",
            "clips_video_container",
            "clips_swipe_refresh_container",
    };
    // IDs dyal l'écran Shorts f YouTube
    private static final String[] YT_SHORTS_IDS = {
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player",
            "reel_watch_fragment_root",
            "shorts_container",
    };

    private static final long COOLDOWN_MS = 800;
    private long lastAction = 0;
    private long lastToast = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();

        long now = SystemClock.uptimeMillis();
        if (now - lastAction < COOLDOWN_MS) return;

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        // TikTok: l app kamla hiya reels -> nrj3o l'écran d'accueil
        for (String tt : TIKTOK) {
            if (tt.equals(pkg)) {
                if (sp.getBoolean(KEY_TT, true)) {
                    block(GLOBAL_ACTION_HOME, "TikTok", sp);
                }
                return;
            }
        }

        boolean isIg = IG.equals(pkg) && sp.getBoolean(KEY_IG, true);
        boolean isYt = YT.equals(pkg) && sp.getBoolean(KEY_YT, true);
        if (!isIg && !isYt) return;

        // Instagram Accueil: ila nzel l ta7t men stories (scroll f l feed), nrj3o l fo9
        if (IG.equals(pkg) && sp.getBoolean(KEY_IG_FEED, true)
                && event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED
                && event.getFromIndex() >= 1) {
            AccessibilityNodeInfo r = getRootInActiveWindow();
            if (r != null && isSelectedId(r, IG + ":id/feed_tab")
                    && !hasAnyId(r, IG, IG_REELS_IDS)) {
                AccessibilityNodeInfo src = event.getSource();
                boolean done = false;
                if (src != null) {
                    // nrj3o l fo9 (stories)
                    for (int i = 0; i < 15 && src.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); i++) {
                        done = true;
                    }
                }
                if (!done) clickId(r, IG + ":id/feed_tab");
                counted("Posts dyal Accueil", sp);
                return;
            }
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        if (root.getPackageName() == null || !pkg.equals(root.getPackageName().toString())) return;

        if (isIg && hasAnyId(root, IG, IG_REELS_IDS)) {
            // nrj3o l feed (Accueil); ila ma l9inahch, "Retour"
            if (!clickId(root, IG + ":id/feed_tab")) {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }
            counted("Reels", sp);
        } else if (isYt && hasAnyId(root, YT, YT_SHORTS_IDS)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            counted("Shorts", sp);
        }
    }

    private boolean hasAnyId(AccessibilityNodeInfo root, String pkg, String[] ids) {
        for (String id : ids) {
            List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(pkg + ":id/" + id);
            if (found != null && !found.isEmpty()) {
                for (AccessibilityNodeInfo n : found) {
                    if (n.isVisibleToUser()) return true;
                }
            }
        }
        return false;
    }

    private boolean isSelectedId(AccessibilityNodeInfo root, String fullId) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(fullId);
        if (found == null) return false;
        for (AccessibilityNodeInfo n : found) {
            if (n.isSelected()) return true;
        }
        return false;
    }

    private boolean clickId(AccessibilityNodeInfo root, String fullId) {
        List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(fullId);
        if (found == null) return false;
        for (AccessibilityNodeInfo n : found) {
            if (n.isVisibleToUser() && n.isClickable()
                    && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
        }
        return false;
    }

    private void block(int action, String what, SharedPreferences sp) {
        performGlobalAction(action);
        counted(what, sp);
    }

    private void counted(String what, SharedPreferences sp) {
        long now = SystemClock.uptimeMillis();
        lastAction = now;
        sp.edit().putInt(KEY_COUNT, sp.getInt(KEY_COUNT, 0) + 1).apply();
        if (now - lastToast > 3000) {
            lastToast = now;
            Toast.makeText(this, "🚫 " + what + " mblokyin. Rje3 l khdmtek 💪", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInterrupt() {
    }
}
