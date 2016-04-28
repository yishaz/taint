package com.coinbase.android;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.coinbase.android.UpdateWidgetBalanceService.WidgetUpdater;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TransactionsAppWidgetProvider extends AppWidgetProvider {

  public static class TransactionsWidgetUpdater implements WidgetUpdater {

    @Override
    public void updateWidget(Context context, AppWidgetManager manager, int appWidgetId, String balance) {
      String accountId = PreferenceManager.getDefaultSharedPreferences(context).getString(
              String.format(Constants.KEY_WIDGET_ACCOUNT, appWidgetId), null);
      Intent intent = new Intent(context, TransactionsRemoteViewsService.class);
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      intent.putExtra(TransactionsRemoteViewsService.ACCOUNT_ID, accountId);
      intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

      RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_transactions);
      if(accountId != null) {
        rv.setRemoteAdapter(appWidgetId, R.id.widget_list, intent);
      } else {
        Log.e("Coinbase", "Could not get account ID for widget " + appWidgetId);
      }
      rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

      Intent intentTemplate = new Intent(context, TransactionDetailsActivity.class);
      PendingIntent pendingIntentTemplate = PendingIntent.getActivity(context, 0, intentTemplate, 0);
      rv.setPendingIntentTemplate(R.id.widget_list, pendingIntentTemplate);

      rv.setTextViewText(R.id.widget_balance, balance);

      WidgetCommon.bindButtons(context, rv, appWidgetId);

      Log.i("Coinbase", "Updating transactions widget " + appWidgetId + " with balance " + balance);
      manager.updateAppWidget(appWidgetId, rv);
    }

  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {

    if(!PlatformUtils.hasHoneycomb()) {
      return; // Transactions widget does not work (adapter widgets were added in Honeycomb)
    }

    for (int i = 0; i < appWidgetIds.length; ++i) {

      if(appWidgetManager.getAppWidgetInfo(appWidgetIds[i]) == null ||
          appWidgetManager.getAppWidgetInfo(appWidgetIds[i]).provider == null ||
          appWidgetManager.getAppWidgetInfo(appWidgetIds[i]).provider.getClassName() == null ||
          !appWidgetManager.getAppWidgetInfo(appWidgetIds[i]).provider.getClassName().equals(getClass().getName())) {
        // Not for us
        Log.w("Coinbase", "Received app widget broadcast for other provider " + appWidgetIds[i]);
        continue;
      } else {
        Log.i("Coinbase", "Updating " + appWidgetIds[i] + " as " + getClass().getSimpleName());
      }

      // Start the service to update the widget
      Intent service = new Intent(context, UpdateWidgetBalanceService.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_UPDATER_CLASS, TransactionsWidgetUpdater.class);
      service.putExtra(UpdateWidgetBalanceService.EXTRA_WIDGET_ID, appWidgetIds[i]);
      context.startService(service);
    }
  }
}
