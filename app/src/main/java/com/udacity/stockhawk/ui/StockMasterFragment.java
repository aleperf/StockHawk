package com.udacity.stockhawk.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Display the widget_list_item of Stocks
 */

public class StockMasterFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final int STOCK_LOADER = 123;
    public static final String TAG = StockMasterFragment.class.getSimpleName();


    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.recycler_view_master)
    RecyclerView stockRecyclerView;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.error)
    TextView error;
    @BindView(R.id.last_update_time)
    TextView mLastUpdateTime;


    private StockAdapter adapter;



    public StockMasterFragment() {
        //required empty constructor;
    }

    public interface UpdateChartOnSwipe {

        void onUpdateChart(String symbol);
    }

    public static StockMasterFragment newInstance() {

        return new StockMasterFragment();

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_master, container, false);
        ButterKnife.bind(this, rootView);
        adapter = new StockAdapter(getActivity(), (StockAdapter.StockAdapterOnClickHandler) getActivity());
        stockRecyclerView.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        stockRecyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration decoration = new DividerItemDecoration(stockRecyclerView.getContext(),
                layoutManager.getOrientation());
        stockRecyclerView.addItemDecoration(decoration);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setRefreshing(true);
        onRefresh();

        QuoteSyncJob.initialize(getContext());
        getActivity().getSupportLoaderManager().initLoader(STOCK_LOADER, null, this);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                String symbol = adapter.getSymbolAtPosition(viewHolder.getAdapterPosition());
                //remove stock from preferences
                PrefUtils.removeStock(getActivity(), symbol);
                //remove stock from database
                getContext().getContentResolver().delete(Contract.Quote.makeUriForStock(symbol), null, null);

                //remove stock from widget
                QuoteSyncJob.updateStockHawkWidget(getContext());

                //update chart if in dual pane
                updateChart(symbol);


            }
        }).attachToRecyclerView(stockRecyclerView);


        return rootView;
    }

    private boolean networkUp() {
        ConnectivityManager cm =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    public void onRefresh() {

        QuoteSyncJob.syncImmediately(getContext());

        if (!networkUp() && adapter.getItemCount() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_network));
            error.setVisibility(View.VISIBLE);
        } else if (!networkUp()) {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), R.string.toast_no_connectivity, Toast.LENGTH_LONG).show();
        } else if (PrefUtils.getStocks(getContext()).size() == 0) {
            swipeRefreshLayout.setRefreshing(false);
            error.setText(getString(R.string.error_no_stocks));
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }


    void addStock(String symbol) {

        if (symbol != null && !symbol.isEmpty()) {
            if (networkUp()) {
                swipeRefreshLayout.setRefreshing(true);
            } else {
                String message = getString(R.string.toast_stock_added_no_connectivity, symbol);
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }

            PrefUtils.addStock(getContext(), symbol);
            QuoteSyncJob.syncImmediately(getContext());

        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
                Contract.Quote.URI,
                Contract.Quote.QUOTE_COLUMNS.toArray(new String[]{}),
                null, null, Contract.Quote.COLUMN_SYMBOL);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        swipeRefreshLayout.setRefreshing(false);

        if (data != null && data.getCount() > 0) {

            error.setVisibility(View.GONE);
            mLastUpdateTime.setVisibility(View.VISIBLE);

        } else {
            error.setVisibility(View.VISIBLE);
            mLastUpdateTime.setVisibility(View.GONE);
            if (networkUp()) {

                error.setText(getString(R.string.error_no_stocks));
            } else {
                error.setText(getString(R.string.error_no_network));
            }
        }
        long timeMillis = PrefUtils.getLastUpdateTime(getContext());
        if (isDataUpdated(timeMillis)) {
            mLastUpdateTime.setText(formatDateMessage(timeMillis));
            mLastUpdateTime.setTextColor(ContextCompat.getColor(getContext(), R.color.last_update_time_ok));
        } else {
            mLastUpdateTime.setText(getString(R.string.warning_old_data));
            mLastUpdateTime.setTextColor(ContextCompat.getColor(getContext(), R.color.material_red_700));
        }
        adapter.setCursor(data);


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        swipeRefreshLayout.setRefreshing(false);
        adapter.setCursor(null);
    }

    private void setDisplayModeMenuItemIcon(MenuItem item) {
        if (PrefUtils.getDisplayMode(getContext())
                .equals(getString(R.string.pref_display_mode_absolute_key))) {
            item.setIcon(R.drawable.ic_percentage);
        } else {
            item.setIcon(R.drawable.ic_dollar);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_activity_settings, menu);
        MenuItem item = menu.findItem(R.id.action_change_units);
        setDisplayModeMenuItemIcon(item);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            PrefUtils.toggleDisplayMode(getContext());
            setDisplayModeMenuItemIcon(item);
            adapter.notifyDataSetChanged();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getSupportLoaderManager().restartLoader(STOCK_LOADER, null, this);
        adapter.notifyDataSetChanged();
    }

    private void updateChart(String symbol) {
        //update chart if in dual pane
        if (getContext().getResources().getBoolean(R.bool.dual_pane)) {
            if (getActivity() instanceof UpdateChartOnSwipe) {
                UpdateChartOnSwipe swipeActivity = (UpdateChartOnSwipe) getActivity();
                swipeActivity.onUpdateChart(symbol);
            }
        }
    }

    /**
     * Build a string message with the date and time of the last successful update
     * @param millis a time in milliseconds
     * @return a message with the date and time of the last successful update
     */

    private String formatDateMessage(long millis) {
        DateFormat timeFormatter =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US);
        Date date = new Date(millis);
        String introDate = getString(R.string.last_update_msg);
        return introDate + timeFormatter.format(date);


    }

    /**
     * Check if the time of the last successful update is more than 12h
     * @param millis a time in milliseconds
     * @return true if less of 12h are passes since the last update, false otherwise.
     */
    private boolean isDataUpdated(long millis) {
        Calendar lastUpdate = Calendar.getInstance();
        lastUpdate.setTimeInMillis(millis);
        Calendar twelveHoursAgo = Calendar.getInstance();
        twelveHoursAgo.add(Calendar.HOUR, -12);

        return !(twelveHoursAgo.compareTo(lastUpdate) > 0);

    }

}
