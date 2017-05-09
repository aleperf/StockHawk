package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.widget.StockHawkWidgetProvider;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

import static com.udacity.stockhawk.R.id.symbol;


public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    private static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(final Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);
        Set<String> invalidStocks = new HashSet<>();
        boolean foundInvalidSymbol = false;
        String lastSymbol = "";

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();
                lastSymbol = symbol;


                Stock stock = quotes.get(symbol);
                if(stock == null){
                    foundInvalidSymbol = true;
                    invalidStocks.add(symbol);
                    continue;
                }
                StockQuote quote = stock.getQuote();

                //Check if a stock isValid and if the Stock is still on the market,
                // if the stock is no longer traded, retrieving its history could cause
                // an exception, because the Yahoo page for this stock no longer exists.
                //Example: GEEK symbol is a valid stock name,
                // last traded in 2015/06/30: the symbol is valid, but no history can
                // be retrieved for this symbol.


                if (isValidStockSymbol(stock) && isStockOnTheMarket(quote.getLastTradeTime())) {


                    float price = quote.getPrice().floatValue();

                    float change = quote.getChange().floatValue();

                    float percentChange = quote.getChangeInPercent().floatValue();

                    // WARNING! Don't request historical data for a stock that doesn't exist!
                    // The request will hang forever X_x


                    List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                    StringBuilder historyBuilder = new StringBuilder();

                    for (HistoricalQuote it : history) {
                        historyBuilder.append(it.getDate().getTimeInMillis());
                        historyBuilder.append(", ");
                        historyBuilder.append(it.getClose());
                        historyBuilder.append("\n");
                    }

                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                    quoteCVs.add(quoteCV);
                } else {
                       foundInvalidSymbol = true;
                       invalidStocks.add(symbol);
                        continue;
                }
            }


            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));


            //update last update time in preferences
            PrefUtils.setLastUpdateTime(context);


            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

            //update widget
            updateStockHawkWidget(context);


        } catch (IOException exception) {

            Timber.e(exception, "Error fetching stock quotes");
            //remove the symbol causing the exception
            PrefUtils.removeStock(context, lastSymbol);
            //notify the user of the invalid symbol
            notifyInvalidSymbolsToTheUser(context, new HashSet<String>(symbol));
        }
        if(foundInvalidSymbol){
            //remove invalid symbols and notify the user
            PrefUtils.removeBatchOfInvalidSymbols(context,invalidStocks);
            notifyInvalidSymbolsToTheUser(context, invalidStocks);
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {

            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());


        }
    }

    /**
     * Check if a stock is still on the market. If more then a month has passed since the stock
     * has been traded the last time, we can safely assume the stock isn't on the market.
     *
     * @param lastTrade the last date at which a stock has been traded
     * @return a boolean representing if a stock is still traded on the market or not
     */
    private static boolean isStockOnTheMarket(Calendar lastTrade) {
        Calendar aMonthAgo = Calendar.getInstance();
        aMonthAgo.add(Calendar.MONTH, -1);

        return !(lastTrade.compareTo(aMonthAgo) < 0);



    }

    /**
     * Check if a stock is valid. In most of cases, the YahooFinance API creates a stock
     * object from a symbol whether or not the stock is valid,
     * but if the stock isn't valid all its fields, included the name,
     * are null.
     *
     * @param stock the stock queried by the YahooFinance API
     * @return true if the symbol of the stock is valid, false otherwise
     */

    private static boolean isValidStockSymbol(Stock stock) {

        return stock.getName() != null ;
    }

    public static void updateStockHawkWidget(Context context){
      AppWidgetManager manager = AppWidgetManager.getInstance(context);
       int widgetIds[] = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, StockHawkWidgetProvider.class));
        manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_list_view);
    }

    private static void notifyInvalidSymbolsToTheUser(final Context context,final  Set<String> symbols){
        String introMessage = context.getString(R.string.invalid_symbols_msg);
        String symbolNames = symbols.toString();
        final String message = introMessage + symbolNames.substring(1, symbolNames.length()-1);
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        // THIS IS MAIN THREAD!
                        Toast.makeText(context,
                                message,
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

    }



}
