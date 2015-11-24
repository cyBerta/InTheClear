package info.guardianproject.intheclear;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link PanicWidgetConfigureActivity PanicWidgetConfigureActivity}
 */
public class PanicWidget extends AppWidgetProvider {

    private static final String TAG = "widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d(TAG, "onUpdate");
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            PanicWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
   //  AppWidgetManager agr = AppWidgetManager.getInstance(context);
        Log.d(TAG, "enabled");
        Intent i = new Intent(context, InTheClearActivity.class);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.panic_widget);
        views.setOnClickPendingIntent(R.id.widgetButton, configPendingIntent);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        //CharSequence widgetText = PanicWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.panic_widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

  }

