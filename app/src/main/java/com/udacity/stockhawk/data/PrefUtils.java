package com.udacity.stockhawk.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.udacity.stockhawk.R;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;

public final class PrefUtils {

    private PrefUtils() {
    }



    public static Set<String> getStocks(Context context) {
        String stocksKey = context.getString(R.string.pref_stocks_key);
        String initializedKey = context.getString(R.string.pref_stocks_initialized_key);
        String[] defaultStocksList = context.getResources().getStringArray(R.array.default_stocks);


        HashSet<String> defaultStocks = new HashSet<>(Arrays.asList(defaultStocksList));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());


        boolean initialized = prefs.getBoolean(initializedKey, false);

        if (!initialized) {

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(initializedKey, true);
            editor.putStringSet(stocksKey, defaultStocks);
            editor.apply();
            return defaultStocks;
        }

        Set<String> stocks = prefs.getStringSet(stocksKey, new HashSet<String>());
        return stocks;

    }

    private static void editStockPref(Context context, String symbol, Boolean add) {
        String key = context.getString(R.string.pref_stocks_key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Set<String> stocks = prefs.getStringSet(key, new HashSet<String>());
        Set<String> newStocks = new HashSet<>(stocks);

        if (add) {
            newStocks.add(symbol);
        } else {
            if (newStocks.contains(symbol)) {
                newStocks.remove(symbol);
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, newStocks);
        editor.apply();

    }

    public static void addStock(Context context, String symbol) {
        editStockPref(context, symbol, true);
    }

    public static void removeStock(Context context, String symbol) {
        editStockPref(context, symbol, false);
    }

    public static String getDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String defaultValue = context.getString(R.string.pref_display_mode_default);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static void toggleDisplayMode(Context context) {
        String key = context.getString(R.string.pref_display_mode_key);
        String absoluteKey = context.getString(R.string.pref_display_mode_absolute_key);
        String percentageKey = context.getString(R.string.pref_display_mode_percentage_key);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayMode = getDisplayMode(context);

        SharedPreferences.Editor editor = prefs.edit();

        if (displayMode.equals(absoluteKey)) {
            editor.putString(key, percentageKey);
        } else {
            editor.putString(key, absoluteKey);
        }

        editor.apply();
    }

    public static void setLastUpdateTime(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Calendar calendar = Calendar.getInstance();
        Long timeMillis = calendar.getTimeInMillis();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(context.getString(R.string.pref_last_update_time_key), timeMillis);
        editor.apply();
    }

    public static  long getLastUpdateTime(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Long timeMillis = prefs.getLong(context.getString(R.string.pref_last_update_time_key), 0);
        return timeMillis;
    }

    public static void removeBatchOfInvalidSymbols(Context context, Set<String> symbols){
        String key = context.getString(R.string.pref_stocks_key);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Set<String> stocks = prefs.getStringSet(key, new HashSet<String>());
        Set<String> newStocks = new HashSet<>(stocks);
        for(String symbol: symbols){
            if(newStocks.contains(symbol)){
                newStocks.remove(symbol);
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(key, newStocks);
        editor.apply();

    }

}
