package com.shadow.apps.ui;

import android.view.View;
import android.view.WindowInsets;

public final class SystemBars {
    private SystemBars() {}

    public static void apply(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    left + insets.getSystemWindowInsetLeft(),
                    top + insets.getSystemWindowInsetTop(),
                    right + insets.getSystemWindowInsetRight(),
                    bottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });

        root.requestApplyInsets();
    }
}
