package com.shadow.apps;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(0, 0, 0);
    private static final int SURFACE = Color.rgb(32, 38, 50);
    private static final int MUTED = Color.rgb(168, 178, 194);
    private static final int ACCENT = Color.rgb(69, 196, 255);
    private static final int BORDER = Color.rgb(48, 59, 75);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(16), dp(20), dp(16), dp(16));

        TextView brand = text("SHADOW", 12, ACCENT, true);
        brand.setLetterSpacing(0.24f);
        page.addView(brand);

        TextView title = text("Shadow Apps", 29, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(7);
        page.addView(title, titleParams);

        TextView subtitle = text("Deine Werkzeuge. Ein Zuhause.", 14, MUTED, false);
        LinearLayout.LayoutParams subtitleParams =
                new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(5);
        subtitleParams.bottomMargin = dp(25);
        page.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.VERTICAL);

        cards.addView(createCard("Shadow Toolbox",
                "System, Dateien und praktische Werkzeuge", "toolbox", "01"));
        cards.addView(createCard("ShadowAI",
                "Dein KI-Assistent mit lokalen und Online-Modellen", "ai", "02"));
        cards.addView(createCard("Shadow Browser",
                "Surfen mit Datenschutz- und Tab-Funktionen", "browser", "03"));

        scroll.addView(cards);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = text("SHADOW APPS  •  VERSION 1.0", 10,
                Color.rgb(105, 116, 139), false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(16);
        page.addView(footer, footerParams);

        com.shadow.apps.ui.SystemBars.apply(page);
        setContentView(page);
    }

    private LinearLayout createCard(String name, String description,
                                    String feature, String number) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(SURFACE);
        normal.setCornerRadius(dp(18));
        normal.setStroke(dp(1), BORDER);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            card.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(0x3345C4FF), normal, null));
        } else {
            card.setBackground(normal);
        }

        card.setClickable(true);
        card.setFocusable(true);
        card.setMinimumHeight(dp(112));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView index = text(number, 12, ACCENT, true);
        top.addView(index, new LinearLayout.LayoutParams(dp(36), -2));

        TextView label = text(name, 17, Color.WHITE, true);
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView arrow = text("›", 27, ACCENT, false);
        arrow.setGravity(Gravity.CENTER);
        top.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(32)));
        card.addView(top);

        TextView detail = text(description, 13, MUTED, false);
        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(7);
        card.addView(detail, detailParams);

        card.setContentDescription(name + ". " + description);
        card.setOnClickListener(v -> {
            Intent intent;
            switch (feature) {
                case "toolbox":
                    intent = new Intent(this, com.shadow.toolbox.MainActivity.class);
                    break;
                case "ai":
                    intent = new Intent(this, com.shadowai.app.MainActivity.class);
                    break;
                case "browser":
                    intent = new Intent(this, de.shadow13041.shadowbrowser.MainActivity.class);
                    break;
                default:
                    return;
            }
            startActivity(intent);
        });

        LinearLayout holder = new LinearLayout(this);
        holder.addView(card, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams holderParams =
                new LinearLayout.LayoutParams(-1, -2);
        holderParams.bottomMargin = dp(12);
        holder.setLayoutParams(holderParams);
        return holder;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
