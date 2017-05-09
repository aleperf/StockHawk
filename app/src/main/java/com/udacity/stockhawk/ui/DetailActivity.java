package com.udacity.stockhawk.ui;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.udacity.stockhawk.R;
import butterknife.BindView;
import butterknife.ButterKnife;


public class DetailActivity extends AppCompatActivity {

    public final static String SYMBOL_STRING_EXTRA = "string extra symbol";

    @BindView(R.id.toolbar_detail)
    Toolbar mToolbar;
    @BindView(R.id.chart_header)
    TextView mCharHeader;
    private String mSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getResources().getBoolean(R.bool.dual_pane)){
            finish();
        }
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);
        mSymbol = getIntent().getStringExtra(SYMBOL_STRING_EXTRA);
        Log.d("uffa5", "sono in detail activity ed extra symbol è" + mSymbol);
        mCharHeader.setText(mSymbol);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        //set content description for up arrow
        getSupportActionBar().setHomeActionContentDescription(getString(R.string.return_home));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FragmentManager fm = getSupportFragmentManager();
        DetailFragment detailFragment = DetailFragment.newInstance(mSymbol);
        if(savedInstanceState == null){

            fm.beginTransaction().add(R.id.detail_fragment_container,
                    detailFragment, DetailFragment.TAG).commit();
        } else {

            DetailFragment fragment  = (DetailFragment) fm.findFragmentByTag(DetailFragment.TAG);
            if(fragment != null){
                fm.beginTransaction().replace(R.id.detail_fragment_container,
                        detailFragment, DetailFragment.TAG).commit();
            } else {
                // detailFragment = DetailFragment.newInstance(mSymbol);
                fm.beginTransaction().add(R.id.detail_fragment_container,
                        detailFragment, DetailFragment.TAG).commit();
            }
        }


    }


}
