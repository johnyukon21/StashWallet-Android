package co.stashsats.wallet.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

public class ThemeUtils {
    public static int getThemeFromNetworkId(final String network, final Context context, final Bundle metadata) {
        String baseTheme = "BitcoinTheme";
        if (!network.equals("mainnet")) {
            baseTheme = "BitcoinTestnetTheme";
        }

        final String finalTheme = applyThemeVariant(baseTheme, metadata);
        return context.getResources().getIdentifier(finalTheme, "style", context.getPackageName());
    }

    private static String applyThemeVariant(String baseTheme, Bundle metadata) {
        // get the "NoActionBar" variant of this theme
        if (metadata != null && metadata.getBoolean("useNoActionBar")) {
            baseTheme += ".NoActionBar";
        }

        return baseTheme;
    }

    public static TypedValue resolveAttribute(final Context context, final int attr) {
        // Resolve the value of an attribute based on the theme used by `context`
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);

        return typedValue;
    }

    public static @ColorInt int resolveColorAccent(final Context context) {
        return resolveAttribute(context, R.attr.colorAccent).data;
    }
}
