package co.stashsats.wallet.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.material.snackbar.Snackbar;
import co.stashsats.session.Session;
import co.stashsats.session.Conversion;
import co.stashsats.wallet.ui.preferences.PrefKeys;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public abstract class LoggedActivity extends GaActivity {

    private Timer mTimer = new Timer();
    private long mStart = System.currentTimeMillis();
    private Snackbar mSnackbar;
    private Timer mOfflineTimer = new Timer();
    private Long mTryingAt = 0L;

    private Disposable networkDisposable, transactionDisposable,
                       loginDisposable, logoutDisposable;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkDisposable != null)
            networkDisposable.dispose();
        if (transactionDisposable != null)
            transactionDisposable.dispose();
        if (loginDisposable != null)
            loginDisposable.dispose();
        if (logoutDisposable != null)
            logoutDisposable.dispose();
    }



    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }


    protected String getBitcoinUnitClean() {
        return Conversion.getUnitKey();
    }

    // for btc and fiat
    protected void setAmountText(final EditText amountText, final boolean isFiat,
                                 final ObjectNode currentAmount) throws ParseException {
        final NumberFormat btcNf = Conversion.getNumberFormat();
        setAmountText(amountText, isFiat, currentAmount, btcNf);
    }


    protected void setAmountText(final EditText amountText, final boolean isFiat, final ObjectNode currentAmount,
                                 final NumberFormat btcOrAssetNf) throws ParseException {
        final NumberFormat us = Conversion.getNumberFormat(8, Locale.US);
        final NumberFormat fiatNf = Conversion.getNumberFormat(2);
        final String fiat = fiatNf.format(us.parse(currentAmount.get("fiat").asText()));
        final String source = currentAmount.get(getBitcoinUnitClean()).asText();
        final String btc = btcOrAssetNf.format(us.parse(source));
        amountText.setText(isFiat ? fiat : btc);
    }

    protected void removeUtxosIfTooBig(final ObjectNode transactionFromUri) {
        if (transactionFromUri.toString().length() <= 200000)
            return;
        if (transactionFromUri.has("utxos")) {
            transactionFromUri.remove("utxos");
        }
        if (transactionFromUri.get("send_all").asBoolean() && transactionFromUri.has("used_utxos")) {
            transactionFromUri.remove("used_utxos");
        }
    }

    protected int getActiveAccount() {
        return 0;
    }

}
