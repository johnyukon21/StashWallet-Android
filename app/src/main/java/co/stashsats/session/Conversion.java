package co.stashsats.session;

import co.stashsats.sdk.data.BalanceData;
import co.stashsats.wallet.ui.UI;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import static co.stashsats.session.Session.getSession;

public class Conversion {

    public static String getFiatCurrency() {
        return getSession().getSettings().getPricing().getCurrency();
    }

    public static String getUnit() {
        final int index = Math.max(UI.UNIT_KEYS_LIST.indexOf(getUnitKey()), 0);
        return UI.UNITS[index];
    }

    public static String getUnitKey() {
        final String unit = getSession().getSettings().getUnit();
        return toUnitKey(unit);
    }

    public static String toUnitKey(final String unit) {
        if (!Arrays.asList(UI.UNITS).contains(unit))
            return UI.UNITS[0].toLowerCase(Locale.US);
        return unit.equals("\u00B5BTC") ? "ubtc" : unit.toLowerCase(Locale.US);
    }

    public static String getFiat(final long satoshi, final boolean withUnit) throws Exception {
        return getFiat(getSession().convertBalance(satoshi), withUnit);
    }

    public static String getBtc(final long satoshi, final boolean withUnit) throws Exception {
        return getBtc(getSession().convertBalance(satoshi), withUnit);
    }

    public static String getFiat(final BalanceData balanceData, final boolean withUnit) {
        try {
            final Double number = Double.parseDouble(balanceData.getFiat());
            return getNumberFormat(2).format(number) + (withUnit ? " " + getFiatCurrency() : "");
        } catch (final Exception e) {
            return "N.A." + (withUnit ? " " + getFiatCurrency() : "");
        }
    }

    public static String getBtc(final BalanceData balanceData, final boolean withUnit) {
        final String converted = balanceData.toObjectNode().get(getUnitKey()).asText();
        final Double number = Double.parseDouble(converted);
        return getNumberFormat().format(number) + (withUnit ? " " + getUnit() : "");
    }

    public static NumberFormat getNumberFormat() {
        switch (getUnitKey()) {
        case "btc":
            return getNumberFormat(8);
        case "mbtc":
            return getNumberFormat(5);
        case "ubtc":
        case "bits":
            return getNumberFormat(2);
        default:
            return getNumberFormat(0);
        }
    }

    public static NumberFormat getNumberFormat(final int decimals) {
        return getNumberFormat(decimals, Locale.getDefault());
    }

    public static NumberFormat getNumberFormat(final int decimals, final Locale locale) {
        final NumberFormat instance = NumberFormat.getInstance(locale);
        instance.setMinimumFractionDigits(decimals);
        instance.setMaximumFractionDigits(decimals);
        return instance;
    }
}
