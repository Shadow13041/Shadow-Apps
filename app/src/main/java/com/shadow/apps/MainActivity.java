package com.shadow.apps;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


public class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 15, 24);
    private static final int SURFACE = Color.rgb(24, 29, 43);
    private static final int MUTED = Color.rgb(160, 171, 194);
    private static final int ACCENT = Color.rgb(116, 140, 255);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(22), dp(34), dp(22), dp(24));

        TextView brand = text("SHADOW", 13, ACCENT, true);
        brand.setLetterSpacing(0.22f);
        page.addView(brand);

        TextView title = text("Shadow Apps", 32, Color.WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(8);
        page.addView(title, tp);

        TextView subtitle = text("Deine Werkzeuge. Ein Zuhause.", 15, MUTED, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(5);
        sp.bottomMargin = dp(28);
        page.addView(subtitle, sp);

        ScrollView scroll = new ScrollView(this);
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
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, -2);
        fp.topMargin = dp(18);
        page.addView(footer, fp);

        com.shadow.apps.ui.SystemBars.apply(page);
        setContentView(page);
    }

    private LinearLayout createCard(String name, String description,
                                    String feature, String number) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SURFACE);
        shape.setCornerRadius(dp(18));
        shape.setStroke(dp(1), Color.rgb(42, 50, 70));
        card.setBackground(shape);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView index = text(number, 12, ACCENT, true);
        top.addView(index, new LinearLayout.LayoutParams(dp(38), -2));

        TextView label = text(name, 19, Color.WHITE, true);
        top.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView arrow = text("›", 30, ACCENT, false);
        arrow.setGravity(Gravity.CENTER);
        top.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(30)));
        card.addView(top);

        TextView detail = text(description, 13, MUTED, false);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(8);
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
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = dp(13);
        holder.setLayoutParams(hp);
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
