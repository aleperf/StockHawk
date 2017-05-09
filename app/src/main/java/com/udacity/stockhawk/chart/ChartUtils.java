package com.udacity.stockhawk.chart;


import android.database.Cursor;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.data.Contract;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import au.com.bytecode.opencsv.CSVReader;



/**
 * Convenience methods for converting CSV data from the database into
 * a format suitable for drawing a chart with the MPChart Library.
 */

public class ChartUtils {

    /**
     * Extract history data from a cursor and convert them to a widget_list_item of Entry objects
     * @param symbolCursor a cursor containing the data about the stock
     *
     * @return a widget_list_item of Entry objects, each Entry contains the timeInMillis representing a data
     * and a price at close  of a stock for that date.
     */
    public static List<Entry> getEntries(Cursor symbolCursor){
        String csvData = getHistory(symbolCursor);
        return convertCsvToChartEntries(csvData);
    }

    /**
     * Extract history data of a stock  as a csv string from a cursor
     * @param symbolCursor a Cursor containing the data for a stock
     * @return a String representing a history as csv.
     */

    private static String getHistory(Cursor symbolCursor){
        String csvData = null;
        //get the history column
        if(symbolCursor.moveToFirst()){
             csvData = symbolCursor.getString(symbolCursor.getColumnIndex(Contract.Quote.COLUMN_HISTORY));
        }

        return csvData;

    }

    /**
     * Convert a csv String to a List of Entry objects
     * @param csvData a String representing the history of a stock as csv
     * @return a widget_list_item of Entry objects each Entry contains the timeInMillis representing a data
     * and a price at close  of a stock for that date.
     */

   private static List<Entry> convertCsvToChartEntries (String csvData){
       if(csvData == null){
           return null;
       }
       List<Entry> entries = new ArrayList<>();
       CSVReader reader = new CSVReader(new StringReader(csvData));

       try{
           List<String[]> data = reader.readAll();
           //the csv data are ordered from most recent to oldest
           //but to build the chart we need to reverse the order.
           for(int i = data.size() - 1; i >= 0; i--){
               String[] nextLine = data.get(i);
               Long timeInMillis = Long.valueOf(nextLine[0]);
               Float priceAtClose = Float.valueOf(nextLine[1]);
               Entry entry = new Entry(timeInMillis, priceAtClose);
               entries.add(entry);}


   } catch(IOException e){
   e.printStackTrace();}

       return entries;
   }

    /**
     * Formatter for charts dates, for every x value return
     * a date string with format yyyy-MM-dd
     *
     */

    public static class DateChartFormatter implements IAxisValueFormatter {

        private SimpleDateFormat mFormatter;

        public DateChartFormatter(){

            mFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        }
        @Override
        public String getFormattedValue(float value, AxisBase axis) {

            return mFormatter.format(new Date((long) value));
        }
    }


}
