package com.udacity.stockhawk.widget;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;




public class StockHawkWidgetService extends RemoteViewsService {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockHawkRemoteViewsFactory(getApplicationContext(), intent);
    }


    class StockHawkRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private int mWidgetId;
        private Cursor mCursor;
        private final DecimalFormat dollarFormatWithPlus;
        private final DecimalFormat dollarFormat;
        private final DecimalFormat percentageFormat;


        public StockHawkRemoteViewsFactory(Context context, Intent intent) {
            this.mContext = context;
            mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
            dollarFormatWithPlus.setPositivePrefix("+$");
            percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
            percentageFormat.setMaximumFractionDigits(2);
            percentageFormat.setMinimumFractionDigits(2);
            percentageFormat.setPositivePrefix("+");

        }

        @Override
        public void onCreate() {


        }

        @Override
        public void onDataSetChanged() {
            if (mCursor != null) {
                mCursor.close();
            }
            final long token = Binder.clearCallingIdentity();
            try {
                mCursor = mContext.getContentResolver().query(Contract.Quote.URI, null, null,
                        null, Contract.Quote.COLUMN_SYMBOL);

            } finally {
                Binder.restoreCallingIdentity(token);
            }


        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            if (mCursor != null) {
                return mCursor.getCount();
            }
            return 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {

            mCursor.moveToPosition(position);
            String symbol = mCursor.getString(Contract.Quote.POSITION_SYMBOL);
            String price = dollarFormat.format(mCursor.getFloat(Contract.Quote.POSITION_PRICE));
            float rawAbsoluteChange = mCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
            float percentageChange = mCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

            //inflate widget_list_item item as a RemoteViews
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);

            //set symbol, price and change
            rv.setTextViewText(R.id.symbol, symbol);
            rv.setTextViewText(R.id.price, price);
            if (rawAbsoluteChange > 0) {
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            } else {
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }

            String change = dollarFormatWithPlus.format(rawAbsoluteChange);
            String percentage = percentageFormat.format(percentageChange / 100);

            if (PrefUtils.getDisplayMode(mContext)
                    .equals(mContext.getString(R.string.pref_display_mode_absolute_key))) {
                rv.setTextViewText(R.id.change, change);
            } else {
                rv.setTextViewText(R.id.change, percentage);
            }
            //set a fill-intent
            Bundle extras = new Bundle();
            extras.putInt(StockHawkWidgetProvider.EXTRA_POSITION, position);
            extras.putString(StockHawkWidgetProvider.EXTRA_SYMBOL, symbol);
            Intent fillIntent = new Intent(StockHawkWidgetProvider.ACTION_OPEN_STOCK_HAWK);
            fillIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.widget_item, fillIntent);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


    }
}
