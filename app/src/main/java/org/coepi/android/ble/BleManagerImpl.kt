package org.coepi.android.ble

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.PublishSubject.create
import org.coepi.android.MainActivity
import org.coepi.android.R
import org.coepi.android.cen.Cen
import org.covidwatch.libcontactrace.BluetoothService
import org.covidwatch.libcontactrace.BluetoothService.LocalBinder
import org.covidwatch.libcontactrace.cen.CenGenerator
import org.covidwatch.libcontactrace.cen.CenVisitor
import org.covidwatch.libcontactrace.cen.GeneratedCen
import org.covidwatch.libcontactrace.cen.ObservedCen
import org.covidwatch.libcontactrace.toBytes
import java.util.UUID

interface BleManager {
    val scanObservable: Observable<Cen>

    fun startAdvertiser(cen: Cen)
    fun stopAdvertiser()

    fun startService(cen: Cen)
    fun stopService()

    fun changeAdvertisedValue(cen: Cen)
}

class BleManagerImpl(
    private val app: Application
) : BleManager {

    override val scanObservable: PublishSubject<Cen> = create()

    private val intent get() = Intent(app, BluetoothService::class.java)

    private var service: BluetoothService? = null

    class DefaultCenGenerator(var cen: org.covidwatch.libcontactrace.cen.Cen? = null) :
        CenGenerator {
        override fun generate(): GeneratedCen {
            return   GeneratedCen(UUID.randomUUID().toBytes())
        }
    }

    private val cenGenerator = DefaultCenGenerator()
    private val cenVisitor = DefaultCenVisitor(app)

    inner class DefaultCenVisitor(private val ctx: Context) : CenVisitor {

        override fun visit(cen: GeneratedCen) {}

        override fun visit(cen: ObservedCen) {
            scanObservable.onNext(Cen(cen.data))
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@BleManagerImpl.service = (service as LocalBinder).service.apply {
                configure(
                    BluetoothService.ServiceConfiguration(
                        cenGenerator,
                        cenVisitor,
                        foregroundNotification()
                    )
                )
                start()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private fun foregroundNotification(): Notification {
        createNotificationChannelIfNeeded()

        val notificationIntent = Intent(app, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            app, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(app, CHANNEL_ID)
            .setContentTitle("Tags is logging")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    /**
     * This notification channel is only required for android versions above
     * android O. This creates the necessary notification channel for the foregroundService
     * to function.
     */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = ContextCompat.getSystemService(
                app, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun changeAdvertisedValue(cen: Cen) {
        cenGenerator.cen = GeneratedCen(cen.bytes)
        service?.updateCen()
    }

    override fun startAdvertiser(cen: Cen) {
        cenGenerator.cen = GeneratedCen(cen.bytes)
        service?.startAdvertiser()
    }

    override fun startService(cen: Cen) {
        cenGenerator.cen = GeneratedCen(cen.bytes)
        app.bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        app.startService(intent)
    }

    override fun stopAdvertiser() {
        service?.stopAdvertiser()
    }

    override fun stopService() {
        app.stopService(intent)
    }

    companion object {
        // CONSTANTS
        private const val CHANNEL_ID = "CovidBluetoothContactChannel"
    }
}
