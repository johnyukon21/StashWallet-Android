package co.stashsats.wallet.ui.transactions;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import co.stashsats.sdk.data.TransactionData;
import co.stashsats.sdk.data.TransactionData.TYPE;
import co.stashsats.session.Session;
import co.stashsats.session.Conversion;
import co.stashsats.wallet.ui.R.color;
import co.stashsats.wallet.ui.R.drawable;
import co.stashsats.wallet.ui.R.id;
import co.stashsats.wallet.ui.R.layout;
import co.stashsats.wallet.ui.R.string;
import co.stashsats.wallet.ui.UI;
import co.stashsats.wallet.ui.components.FontAwesomeTextView;
import co.stashsats.wallet.ui.transactions.ListTransactionsAdapter.ViewHolder;

import java.text.DateFormat;
import java.util.List;

public class ListTransactionsAdapter extends Adapter<ViewHolder> {

    private final List<TransactionData> mTxItems;
    private final Activity mActivity;
    private int currentBlock = 0;
    private final OnTxSelected mOnTxSelected;

    @FunctionalInterface
    public interface OnTxSelected {
        void onSelected(final TransactionData tx);
    }

    public ListTransactionsAdapter(final Activity activity,
                                   final List<TransactionData> txItems,
                                   final OnTxSelected selector) {
        mTxItems = txItems;
        mActivity = activity;
        mOnTxSelected = selector;
    }

    public void setCurrentBlock(final int currentBlock) {
        this.currentBlock = currentBlock;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                              .inflate(layout.list_element_transaction, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (position >= mTxItems.size()) {
            return;
        }
        final TransactionData txItem = mTxItems.get(position);

        holder.textValue.setText(getAmountWithUnit(txItem));

        holder.textWhen.setTextColor(ContextCompat.getColor(mActivity, color.tertiaryTextColor));
        holder.textWhen.setText(txItem.getLocalizedDate(DateFormat.MEDIUM));

        final boolean replaceable = txItem.getCanRbf() && txItem.getTxType() != TYPE.IN;
        UI.showIf(replaceable, holder.imageReplaceable);

        final String message;
        if (TextUtils.isEmpty(txItem.getMemo())) {
            if (txItem.getTxType() == TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(
                                            string.id_redeposited), "");
            else if (txItem.getTxType() == TYPE.IN)
                message = String.format("%s %s", mActivity.getString(
                                            string.id_received), "");
            else
                message = txItem.getAddressee();
        } else {
            if (txItem.getTxType() == TYPE.REDEPOSIT)
                message = String.format("%s %s", mActivity.getString(string.id_redeposited), txItem.getMemo());
            else
                message = txItem.getMemo();
        }
        holder.textWho.setText(message);

        final String confirmations;
        final int confirmationsColor;
        if (txItem.getConfirmations(currentBlock) == 0) {
            confirmations = mActivity.getString(string.id_unconfirmed);
            confirmationsColor = color.red;
        } else if (!txItem.hasEnoughConfirmations(currentBlock)) {
            confirmations = mActivity.getString(string.id_d6_confirmations, txItem.getConfirmations(currentBlock));
            confirmationsColor = color.grey_light;
        } else {
            confirmations = mActivity.getString(string.id_completed);
            confirmationsColor = color.grey_light;
        }

        holder.listNumberConfirmation.setText(confirmations);
        holder.listNumberConfirmation.setTextColor(getColor(confirmationsColor));

        final int amountColor;
        final int sentOrReceive;
        if (txItem.getTxType() == TYPE.IN) {
            amountColor = color.green;
            sentOrReceive= drawable.ic_received;
        } else {
            amountColor = color.white;
            sentOrReceive= drawable.ic_sent;
        }
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            holder.sentOrReceive.setImageDrawable(mActivity.getResources().getDrawable(sentOrReceive,
                                                                                       mActivity.getTheme()));
        } else {
            holder.sentOrReceive.setImageDrawable(mActivity.getResources().getDrawable(sentOrReceive));
        }
        holder.textValue.setTextColor(getColor(amountColor));
        holder.mainLayout.setOnClickListener(v -> {
            if (mOnTxSelected != null)
                mOnTxSelected.onSelected(txItem);
        });
    }

    public String getAmountWithUnit(final TransactionData tx) {
        try {
            if (tx.getTxType() == TYPE.REDEPOSIT) {
                final String fee = Conversion.getBtc(tx.getFee(), true);
                return String.format("-%s", fee);
            }
            final String amount = Conversion.getBtc(tx.getSatoshi().get("btc"), true);
            return String.format("%s%s", tx.getTxType() == TYPE.OUT ? "-" : "", amount);
        } catch (final Exception e) {
            Log.e("", "Conversion error: " + e.getLocalizedMessage());
            return "";
        }
    }

    private int getColor(final int resource) {
        return mActivity.getResources().getColor(resource);
    }

    @Override
    public int getItemCount() {
        return mTxItems == null ? 0 : mTxItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView listNumberConfirmation;
        public final TextView textValue;
        public final TextView textWhen;
        public final ImageView imageReplaceable;
        public final FontAwesomeTextView unitText;
        public final TextView textWho;
        public final LinearLayout mainLayout;
        public final ImageView sentOrReceive;

        public ViewHolder(final View v) {

            super(v);

            textValue = UI.find(v, id.listValueText);
            textWhen = UI.find(v, id.listWhenText);
            imageReplaceable = UI.find(v, id.listReplaceableIcon);
            textWho = UI.find(v, id.listWhoText);
            mainLayout = UI.find(v, id.list_item_layout);
            sentOrReceive = UI.find(v, id.imageSentOrReceive);
            unitText = null;
            listNumberConfirmation = UI.find(v, id.listNumberConfirmation);
        }
    }
}
