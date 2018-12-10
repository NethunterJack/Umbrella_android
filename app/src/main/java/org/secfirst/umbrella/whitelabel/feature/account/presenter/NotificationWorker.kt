package org.secfirst.umbrella.whitelabel.feature.account.presenter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import org.secfirst.umbrella.whitelabel.misc.AppExecutors.Companion.ioContext
import org.secfirst.umbrella.whitelabel.misc.launchSilent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NotificationWorker(private val test: Context, workerParams: WorkerParameters) : Worker(test, workerParams) {

    override fun doWork(): Result {
        var result = Result.Success.success()
        launchSilent(ioContext) {
            try {
                makeStatusNotification(System.currentTimeMillis().toString(), test)
                val responseBody = retrofit().getFeedList("en", "1", "").await()
                val feeds = Gson().fromJson(responseBody.string(), Array<FeedItemResponse>::class.java)
                print("Notification worker success.")
            } catch (exception: Exception) {
                print("Notification worker error.")
                result = Result.Failure.failure()
            }
        }
        return result
    }


    private fun retrofit() = providePostApi(Retrofit.Builder()
            .baseUrl(NetworkEndPoint.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build())

    private fun providePostApi(retrofit: Retrofit): ApiHelper {
        return retrofit.create(ApiHelper::class.java)
    }

    private fun makeStatusNotification(message: String, context: Context) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = "Name of the notification"
            val description = "Description of the notification"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("1", name, importance)
            channel.description = description

            // Add the channel
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager?.createNotificationChannel(channel)
        }

        // Create the notification
        val builder = NotificationCompat.Builder(context, "1")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(message)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(LongArray(0))

        // Show the notification
        NotificationManagerCompat.from(context).notify(1, builder.build())
    }

}