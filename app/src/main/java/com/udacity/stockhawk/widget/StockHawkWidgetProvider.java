package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.ui.DetailActivity;
import com.udacity.stockhawk.ui.DetailFragment;
import com.udacity.stockhawk.ui.MainActivity;


public class StockHawkWidgetProvider extends AppWidgetProvider {

    public static final String EXTRA_POSITION = "com.udacity.stockhawk.widget.extra_position";
    public static final String EXTRA_SYMBOL = "com.udacity.stockhawk.widget.extra_symbol";
    public static final String ACTION_OPEN_STOCK_HAWK = "com.udacity.stockhawk.widget.open_stock_hawk";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (action.equals(ACTION_OPEN_STOCK_HAWK)) {
            String stockSymbol = intent.getStringExtra(EXTRA_SYMBOL);
            Intent mainAppIntent = new Intent(context, DetailActivity.class);
            mainAppIntent.putExtra(DetailActivity.SYMBOL_STRING_EXTRA, stockSymbol);
            mainAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context); //new
            taskStackBuilder.addParentStack(MainActivity.class);//new
            taskStackBuilder.addNextIntent(mainAppIntent);
            context.startActivity(mainAppIntent);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int i = 0; i < appWidgetIds.length; i++) {
            Intent intent = new Intent(context, StockHawkWidgetService.class);
            // Add the app widget ID to the intent extras.
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            // Instantiate the RemoteViews object for the app widget layout.
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_stock_hawk);

            rv.setRemoteAdapter(R.id.widget_list_view, intent);
            rv.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);

            //set the action for the intent
            Intent stockIntent = new Intent(context, StockHawkWidgetProvider.class);
            stockIntent.setAction(ACTION_OPEN_STOCK_HAWK);
            stockIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            stockIntent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            PendingIntent stockPendingIntent = PendingIntent.getBroadcast(context, appWidgetIds[i], stockIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            rv.setPendingIntentTemplate(R.id.widget_list_view, stockPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }


}
