package com.omar.reelstop;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;
    private TextView counter;
    private Button enableBtn;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences(BlockerService.PREFS, MODE_PRIVATE);
        status = findViewById(R.id.status);
        counter = findViewById(R.id.counter);
        enableBtn = findViewById(R.id.enable_btn);

        enableBtn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        bindSwitch(R.id.sw_instagram, BlockerService.KEY_IG);
        bindSwitch(R.id.sw_instagram_feed, BlockerService.KEY_IG_FEED);
        bindSwitch(R.id.sw_youtube, BlockerService.KEY_YT);
        bindSwitch(R.id.sw_tiktok, BlockerService.KEY_TT);
    }

    private void bindSwitch(int id, String key) {
        Switch sw = findViewById(id);
        sw.setChecked(sp.getBoolean(key, true));
        sw.setOnCheckedChangeListener((b, checked) -> sp.edit().putBoolean(key, checked).apply());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = isServiceEnabled();
        status.setText(on ? "✅ ReelStop khddam" : "❌ ReelStop mtfi");
        status.setTextColor(on ? 0xFF5EE08A : 0xFFFF6B6B);
        enableBtn.setText(on ? "Paramètres d'accessibilité" : "Activer ReelStop");
        counter.setText("7bestek " + sp.getInt(BlockerService.KEY_COUNT, 0) + " merra 💪");
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName me = new ComponentName(this, BlockerService.class);
        for (String s : enabled.split(":")) {
            ComponentName c = ComponentName.unflattenFromString(s);
            if (me.equals(c)) return true;
        }
        return false;
    }
}
