package com.udacity.stockhawk.ui;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;

import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.chart.ChartUtils;
import com.udacity.stockhawk.data.Contract;

import java.util.List;


import butterknife.BindView;
import butterknife.ButterKnife;

import static com.udacity.stockhawk.R.id.symbol;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = DetailFragment.class.getSimpleName();
    private String mSymbol;
    private final static int DETAIL_LOADER_ID = 345;
    public final static String EXTRA_SYMBOL = "com.udacity.stockhawk.ui.extra.symbol";
    public static final String DEFAULT_STOCK = "stock hawk default stock";


    @BindView(R.id.chart)
    LineChart mChart;
    @BindView(R.id.empty_chart_text_view)
    TextView mEmptyChartTextView;
    @BindView(R.id.empty_chart_image_view)
    ImageView mEmptyImageView;

    public DetailFragment() {
        //default constructor
    }


    public static DetailFragment newInstance(String symbol) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_SYMBOL, symbol);
        DetailFragment fragment = new DetailFragment();
        fragment.setArguments(bundle);
        return fragment;

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSymbol = getArguments().getString(EXTRA_SYMBOL, DEFAULT_STOCK);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, rootView);
        getActivity().getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, null, this);
        mChart.setContentDescription(getString(R.string.chart_description) + mSymbol);

        return rootView;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Uri symbolUri = Contract.Quote.makeUriForStock(mSymbol);
        return new CursorLoader(getContext(), symbolUri, null, null, null, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null && data.getCount() > 0) {

            mEmptyChartTextView.setVisibility(View.GONE);
            mEmptyImageView.setVisibility(View.GONE);
            mChart.setVisibility(View.VISIBLE);

            List<Entry> entries = ChartUtils.getEntries(data);
            String chartLabel = mSymbol + getString(R.string.chart_label);
            LineDataSet dataSet = new LineDataSet(entries, chartLabel);

            dataSet.setDrawValues(true);
            LineData lineData = new LineData(dataSet);
            mChart.setData(lineData);

            //styling the chart

            mChart.fitScreen();
            //hide the "description label" default text
            Description description = new Description();
            description.setText("");
            mChart.setDescription(description);
            mChart.setDrawBorders(true);
            Legend legend = mChart.getLegend();
            legend.setTextSize((float) getResources().getInteger(R.integer.legend_text_size));
            legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            legend.setStackSpace(12);
            legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
            legend.setDrawInside(false);
            XAxis xAxis = mChart.getXAxis();
            xAxis.setValueFormatter(new ChartUtils.DateChartFormatter());
            xAxis.setLabelCount(getResources().getInteger(R.integer.max_visible_date_labels));

            mChart.invalidate();
        } else { // if no data available
            mChart.setVisibility(View.INVISIBLE);
            mEmptyChartTextView.setVisibility(View.VISIBLE);
            mEmptyImageView.setVisibility(View.VISIBLE);

        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //do nothing
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getSupportLoaderManager().restartLoader(DETAIL_LOADER_ID, null, this);
    }



}
