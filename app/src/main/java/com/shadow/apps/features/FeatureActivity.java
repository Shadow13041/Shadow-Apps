package com.shadow.apps.features;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeatureActivity extends Activity {
    public static final String EXTRA_FEATURE = "com.shadow.apps.EXTRA_FEATURE";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        String feature = getIntent().getStringExtra(EXTRA_FEATURE);
        String title;
        String description;

        if ("toolbox".equals(feature)) {
            title = "Shadow Toolbox";
            description = "Der Werkzeugbereich wird hier integriert.";
        } else if ("ai".equals(feature)) {
            title = "ShadowAI";
            description = "Der KI-Bereich wird hier integriert.";
        } else if ("browser".equals(feature)) {
            title = "Shadow Browser";
            description = "Der Browserbereich wird hier integriert.";
        } else {
            title = "Shadow Apps";
            description = "Unbekannter Bereich.";
        }

        int bg = Color.rgb(12, 15, 24);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(22));
        page.setBackgroundColor(bg);

        TextView back = new TextView(this);
        back.setText("‹  Zurück");
        back.setTextColor(Color.rgb(145, 164, 255));
        back.setTextSize(16);
        back.setPadding(0, dp(8), 0, dp(20));
        back.setOnClickListener(v -> finish());
        page.addView(back);

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(27);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        page.addView(heading);

        TextView info = new TextView(this);
        info.setText(description);
        info.setTextColor(Color.rgb(160, 171, 194));
        info.setTextSize(15);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(12);
        page.addView(info, p);

        setContentView(page);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
