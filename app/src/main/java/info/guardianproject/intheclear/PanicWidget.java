package info.guardianproject.intheclear;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link PanicWidgetConfigureActivity PanicWidgetConfigureActivity}
 */
public class PanicWidget extends AppWidgetProvider {

    private static final String TAG = "widget";
    private boolean startAction;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action  = intent.getAction();
        if (action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)){
            if (intent.getIntExtra(ITCConstants.Widget.START_PANIC_ACTION, 0) != 0){
                startAction = true;
            } else {
                startAction = false;
            }
        }

        super.onReceive(context, intent);

    }



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them

        ComponentName thisWidget = new ComponentName(context, PanicWidget.class);
        int[] allPanicWidgetsIds = appWidgetManager.getAppWidgetIds(thisWidget);


        final int N = allPanicWidgetsIds.length;

        if (startAction == true){
            Intent intent = new Intent(context, PanicActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } else {
            startAction = true;
        }

        for (int i = 0; i < N; i++) {
//            Log.d(TAG, "onUpdate for Widget no: " + i + " (" + allPanicWidgetsIds[i] + ")");
          //  updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.panic_widget);

            //views.setTextViewText(R.id.appwidget_text, widgetText);

            Intent intent2 = new Intent(context, PanicWidget.class);
            intent2.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allPanicWidgetsIds);
            intent2.putExtra(ITCConstants.Widget.START_PANIC_ACTION, 1);


            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(allPanicWidgetsIds[i], views);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            //PanicWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");
   //  AppWidgetManager agr = AppWidgetManager.getInstance(context);
   /*   Log.d(TAG, "enabled");
        Intent i = new Intent(context, InTheClearActivity.class);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, i, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.panic_widget);
        views.setOnClickPendingIntent(R.id.widgetButton, configPendingIntent);*/
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.d(TAG, "onDisabled");

    }

  /*  static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int[] appWidgetIds) {

        //CharSequence widgetText = PanicWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.panic_widget);
        //views.setTextViewText(R.id.appwidget_text, widgetText);

        Intent intent = new Intent(context, PanicWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetId)

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }*/

  }

