package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import com.udacity.stockhawk.R;

public class MainActivity extends AppCompatActivity implements StockAdapter.StockAdapterOnClickHandler,
        AddStockDialog.OnAddingStock, StockMasterFragment.UpdateChartOnSwipe {

    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.toolbar_main)
    Toolbar mToolbar;
    @BindView(R.id.fab_add_stock)
    FloatingActionButton mFabAddStock;
    private StockMasterFragment mMasterFragment;
    private String mActualStock;
    public final static String ACTION_OPEN_DETAIL_STOCK = "com.udacity.stockhawk.ui.OPEN_DETAIL_STOCK";
    public final static String ACTUAL_STOCK_VALUE = "actual stock value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        boolean dualPane = getResources().getBoolean(R.bool.dual_pane);
        mActualStock = getActualStock(savedInstanceState);

        if (savedInstanceState == null) { //no fragment attached
            mMasterFragment = new StockMasterFragment();
            transaction.add(R.id.master_fragment_container, mMasterFragment, StockMasterFragment.TAG);

            if (dualPane) {
                DetailFragment detailFragment = DetailFragment.newInstance(mActualStock);
                transaction.add(R.id.detail_fragment_container, detailFragment, DetailFragment.TAG);
            }
        } else {
            mMasterFragment = (StockMasterFragment) manager.findFragmentByTag(StockMasterFragment.TAG);
            transaction.replace(R.id.master_fragment_container, mMasterFragment, StockMasterFragment.TAG);

            if (dualPane) {
                DetailFragment detailFragment = (DetailFragment) manager.findFragmentByTag(DetailFragment.TAG);
                if (detailFragment != null) {
                    transaction.replace(R.id.detail_fragment_container, detailFragment, DetailFragment.TAG);
                } else {
                    detailFragment = DetailFragment.newInstance(mActualStock);
                    transaction.add(R.id.detail_fragment_container, detailFragment, DetailFragment.TAG);
                }
            }

        }

        transaction.commit();


    }


    @Override
    public void onClick(String symbol) {
        mActualStock = symbol;
        Timber.d("Symbol clicked: %s", symbol);
        boolean dualPane = getResources().getBoolean(R.bool.dual_pane);
        if (dualPane) {
            DetailFragment detailFragment = DetailFragment.newInstance(symbol);
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(R.id.detail_fragment_container,
                    detailFragment, detailFragment.TAG).commit();

        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailActivity.SYMBOL_STRING_EXTRA, symbol);
            startActivity(intent);
        }
    }

    public void button(@SuppressWarnings("UnusedParameters") View view) {
        new AddStockDialog().show(getFragmentManager(), "StockDialogFragment");
    }


    @Override
    public void addStock(String stock) {
        mMasterFragment.addStock(stock);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACTUAL_STOCK_VALUE, mActualStock);

    }

    private String getActualStock(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return savedInstanceState.getString(ACTUAL_STOCK_VALUE);
        } else return DetailFragment.DEFAULT_STOCK;
    }

    @Override
    public void onUpdateChart(String symbol) {

        if (symbol.equals(mActualStock)) {
            mActualStock = DetailFragment.DEFAULT_STOCK;

            FragmentManager manager = getSupportFragmentManager();
            DetailFragment detailFragment = DetailFragment.newInstance(mActualStock);
            manager.beginTransaction().replace(R.id.detail_fragment_container,
                    detailFragment, DetailFragment.TAG).commit();
        }

    }
}