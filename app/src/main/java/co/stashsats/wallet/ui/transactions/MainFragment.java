package co.stashsats.wallet.ui.transactions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.stashsats.sdk.data.SubaccountData;
import co.stashsats.sdk.data.TransactionData;
import co.stashsats.wallet.ui.GAFragment;
import co.stashsats.wallet.ui.GaActivity;
import co.stashsats.wallet.ui.R;
import co.stashsats.wallet.ui.UI;
import co.stashsats.wallet.ui.accounts.AccountView;
import co.stashsats.wallet.ui.components.BottomOffsetDecoration;
import co.stashsats.wallet.ui.components.DividerItem;
import co.stashsats.wallet.ui.preferences.PrefKeys;
import co.stashsats.wallet.ui.receive.ReceiveActivity;
import co.stashsats.wallet.ui.send.ScanActivity;

import org.bitcoinj.core.Sha256Hash;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.TimeoutException;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;
import co.stashsats.session.Session;
import co.stashsats.session.Conversion;
import static co.stashsats.wallet.ui.TabbedMainActivity.REQUEST_TX_DETAILS;



public class MainFragment extends GAFragment implements View.OnClickListener, ListTransactionsAdapter.OnTxSelected {

    private static final String TAG = MainFragment.class.getSimpleName();
    private static final int TX_PER_PAGE = 15;

    private final List<TransactionData> mTxItems = new ArrayList<>();
    private int mActiveAccount = 0;
    private SubaccountData mSubaccount;
    private Integer mPageLoaded = 0;
    private Integer mLastPage = Integer.MAX_VALUE;
    private boolean isLoading = false;

    private AccountView mAccountView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListTransactionsAdapter mTransactionsAdapter;
    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
    private View mView;

    private Disposable newTransactionDisposable;
    private Disposable blockDisposable;
    private Disposable subaccountDisposable;
    private Disposable transactionDisposable;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.d(TAG, "onCreateView -> " + TAG);

        mView = inflater.inflate(R.layout.fragment_main, container, false);
        if (isZombie())
            return mView;

        // Setup recycler & adapter
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.setHasFixedSize(true);
        txView.addItemDecoration(new DividerItem(getActivity()));
        txView.setLayoutManager(mLayoutManager);
        float offsetPx = getResources().getDimension(R.dimen.adapter_bar);
        final BottomOffsetDecoration bottomOffsetDecoration = new BottomOffsetDecoration((int) offsetPx);
        txView.addItemDecoration(bottomOffsetDecoration);
        mTransactionsAdapter = new ListTransactionsAdapter(getGaActivity(),  mTxItems, this);
        txView.setAdapter(mTransactionsAdapter);
        txView.addOnScrollListener(recyclerViewOnScrollListener);

        // FIXME, more efficient to use swap
        // txView.swapAdapter(lta, false);
        mSwipeRefreshLayout = UI.find(mView, R.id.mainTransactionListSwipe);
        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.accent));
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "onRefresh -> " + TAG);
            update();
        });

        // Setup account card view
        mAccountView = UI.find(mView, R.id.accountView);
        mAccountView.setOnClickListener(this);




        return mView;
    }

//    public static TorInitializationListener createInitalizationListner() {
//        return new TorInitializationListener() {
//            @Override
//            public void initializationProgress(String message, int percent) {
//                System.out.println(">>> [ " + percent + "% ]: " + message);
//            }
//
//            @Override
//            public void initializationCompleted() {
//                System.out.println("Tor is ready to go!");
//            }
//        };
//    }

    @Override
    public void onPause() {
        super.onPause();
        if (isZombie())
            return;
        if (blockDisposable != null)
            blockDisposable.dispose();
        if (transactionDisposable != null)
            transactionDisposable.dispose();
        if (subaccountDisposable != null)
            subaccountDisposable.dispose();
        if (newTransactionDisposable != null)
            newTransactionDisposable.dispose();
    }

    @Override
    public void onResume () {
        super.onResume();
        if (isZombie())
            return;

//        // on new block received
//        blockDisposable = Session.getSession().getNotificationModel().getBlockObservable()
//                          .observeOn(AndroidSchedulers.mainThread())
//                          .subscribe((blockHeight) -> {
//            mTransactionsAdapter.setCurrentBlock(blockHeight);
//            mTransactionsAdapter.notifyDataSetChanged();
//        });
//
//        // on new transaction received
//        newTransactionDisposable = Session.getSession().getNotificationModel().getTransactionObservable()
//                                   .observeOn(AndroidSchedulers.mainThread())
//                                   .subscribe((transaction) -> {
//            update();
//        });

        // Update information
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);

        update();
    }

    private void update() {
        subaccountDisposable = Observable.just(Session.getSession())
                               .observeOn(Schedulers.computation())
                               .map((session) -> {
            return Session.getSession().getSubAccount(getGaActivity(), mActiveAccount);
        })
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe((subaccount) -> {
            mSubaccount = subaccount;
            final Map<String, Long> balance = getBalance();
            mAccountView.setTitle("Balance");
            mAccountView.setBalance(balance.get("btc").longValue());

            // Load transactions after subaccount data because
            // ledger HW doesn't support parallel operations
            updateTransactions(true);
        }, (final Throwable e) -> {
            Log.d(TAG, e.getLocalizedMessage());
        });
    }

    private void updateTransactions(final boolean clean) {
        if (clean) {
            mPageLoaded = 0;
            mLastPage = Integer.MAX_VALUE;
        }
        transactionDisposable = Observable.just(Session.getSession())
                                .observeOn(Schedulers.computation())
                                .map((session) -> {
            return getTransactions(mActiveAccount);
        })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((transactions) -> {
            isLoading = false;
            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(false);

            final Sha256Hash oldTop = !mTxItems.isEmpty() ? mTxItems.get(0).getTxhashAsSha256Hash() : null;
            if (clean)
                mTxItems.clear();
            mTxItems.addAll(transactions);
            mTransactionsAdapter.setCurrentBlock(Session.getSession().getBlockHeight());
            mTransactionsAdapter.notifyDataSetChanged();
            showTxView(!mTxItems.isEmpty());

            final Sha256Hash newTop = !mTxItems.isEmpty() ? mTxItems.get(0).getTxhashAsSha256Hash() : null;
            if (oldTop != null && newTop != null && !oldTop.equals(newTop)) {
                // A new tx has arrived; scroll to the top to show it
                final RecyclerView recyclerView = UI.find(mView, R.id.mainTransactionList);
                recyclerView.smoothScrollToPosition(0);
            }
        }, (final Throwable e) -> {
            Log.d(TAG, e.getLocalizedMessage());
            isLoading = false;
            if (mSwipeRefreshLayout != null)
                mSwipeRefreshLayout.setRefreshing(false);
        });
    }

    private List<TransactionData> getTransactions(final int subaccount) throws Exception {
        final List<TransactionData> txs = Session.getSession().getTransactions(
            getGaActivity(), subaccount, mPageLoaded * TX_PER_PAGE, TX_PER_PAGE);
        if (txs.size() < TX_PER_PAGE)
            mLastPage = mPageLoaded;
        mPageLoaded++;
        return txs;
    }

    private Map<String, Long> getBalance() {
        if (mSubaccount == null)
            return new HashMap<String, Long>();
        return mSubaccount.getSatoshi();
    }

    // TODO: Called when a new verified transaction is seen
    public void onVerifiedTx(final Observer observer) {
        final RecyclerView txView = UI.find(mView, R.id.mainTransactionList);
        txView.getAdapter().notifyDataSetChanged();
    }

    private void showTxView(final boolean doShowTxList) {
        UI.showIf(doShowTxList, UI.find(mView, R.id.mainTransactionList));
        UI.showIf(!doShowTxList, UI.find(mView, R.id.emptyListText));
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        final GaActivity activity = getGaActivity();
        if (isVisibleToUser && activity != null) {
            activity.hideKeyboardFrom(null); // Current focus
        }
    }

    @Override
    public void onClick(final View view) {
        view.setEnabled(false);
        if (view.getId() == R.id.receiveButton) {
            view.setEnabled(true);
            final Intent intent = new Intent(getActivity(), ReceiveActivity.class);
            final ObjectMapper mObjectMapper = new ObjectMapper();
            try {
                final String text = mObjectMapper.writeValueAsString(mSubaccount);
                intent.putExtra("SUBACCOUNT", text);
                getActivity().startActivity(intent);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else if (view.getId() == R.id.sendButton) {
            view.setEnabled(true);
            final Intent intent = new Intent(getActivity(), ScanActivity.class);
            startActivity(intent);
        }
    }

    private void loadMoreItems() {
        Log.d(TAG, "loadMoreItems");
        isLoading = true;
        updateTransactions(false);
    }

    private RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
            super.onScrolled(recyclerView, dx, dy);
            final int visibleItemCount = mLayoutManager.getChildCount();
            final int totalItemCount = mLayoutManager.getItemCount();
            final int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
            final boolean isLastPage = mPageLoaded >= mLastPage;
            if (!isLoading && !isLastPage) {
                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0) {
                    loadMoreItems();
                }
            }
        }
    };

    @Override
    public void onSelected(final TransactionData tx) {
        final Intent txIntent = new Intent(getActivity(), TransactionActivity.class);
        final HashMap<String, Long> balance = new HashMap<String, Long>(getBalance());
        txIntent.putExtra("TRANSACTION", tx);
        txIntent.putExtra("BALANCE", balance);
        startActivityForResult(txIntent, REQUEST_TX_DETAILS);
    }
}
