package org.secfirst.umbrella.whitelabel.feature.account.presenter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import androidx.work.Result
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.experimental.CoroutineCallAdapterFactory
import org.secfirst.umbrella.whitelabel.R
import org.secfirst.umbrella.whitelabel.data.network.ApiHelper
import org.secfirst.umbrella.whitelabel.data.network.FeedItemResponse
import org.secfirst.umbrella.whitelabel.data.network.NetworkEndPoint
import org.secfirst.umbrella.whitelabel.feature.main.MainActivity
import org.secfirst.umbrella.whitelabel.misc.AppExecutors.Companion.uiContext
import org.secfirst.umbrella.whitelabel.misc.launchSilent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


class NotificationWorker(private val appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        var result = Result.Success.success()
        val countryCode = inputData.getString(EXTRA_COUNTRY_CODE) ?: ""
        val sources = inputData.getString(EXTRA_SOURCES) ?: ""
        launchSilent(uiContext) {
            try {
                val responseBody = retrofit().getFeedList(countryCode, sources, "0").await()
                val feedsItem = Gson()
                        .fromJson(responseBody.string(), Array<FeedItemResponse>::class.java)
                        .toList()
                if (feedsItem.isNotEmpty())
                    makeStatusNotification(makeNotificationTitle(feedsItem), appContext)

            } catch (e: Exception) {
                result = Result.Failure.failure()
            }
        }

        return result
    }

    private fun makeNotificationTitle(feedsItem: List<FeedItemResponse>): String {
        var notificationTitle = ""
        if (feedsItem.isNotEmpty()) {
            for (index in 0..1) {
                val title = feedsItem[index].title
                notificationTitle += "$title\n\n"
            }
        }
        return notificationTitle
    }

    private fun retrofit() = providePostApi(Retrofit.Builder()
            .baseUrl(NetworkEndPoint.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build())

    private fun providePostApi(retrofit: Retrofit): ApiHelper {
        return retrofit.create(ApiHelper::class.java)
    }

    private fun intent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java)
        val flags = PendingIntent.FLAG_CANCEL_CURRENT
        intent.putExtra(NOTIFICATION_CLICKED, true)
        return PendingIntent.getActivity(appContext, NOTIFICATION_ID, intent, flags)
    }

    private fun makeStatusNotification(message: String, context: Context) {
        val title = appContext.getText(R.string.notification_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, title, importance)
            channel.description = message
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentIntent(intent())
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder)
    }

    companion object {
        const val NOTIFICATION_CLICKED = "notification"
        const val EXTRA_COUNTRY_CODE = "feed_sources"
        const val EXTRA_SOURCES = "location"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "1"
    }

}