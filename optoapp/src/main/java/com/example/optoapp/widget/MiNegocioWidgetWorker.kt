package com.example.optoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.optoapp.MainActivity
import com.example.optoapp.R
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.util.formatAsCurrency
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class MiNegocioWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: ResumenDiarioDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val today = LocalDate.now().toString()

        val opticaId = readOpticaId(context)
        if (opticaId.isBlank()) return ListenableWorker.Result.success()

        val entity = try {
            dao.getByOpticaAndDate(opticaId, today)
        } catch (e: Exception) {
            null
        }

        val appWidgetIds = getAppWidgetManager(context).getAppWidgetIds(
            ComponentName(context, MiNegocioWidgetProvider::class.java)
        )

        return doWorkCore(context, entity, today, appWidgetIds) { ids, views ->
            getAppWidgetManager(context).updateAppWidget(ids, views)
        }
    }

    companion object {
        fun doWorkCore(
            context: Context,
            entity: ResumenDiarioEntity?,
            today: String,
            appWidgetIds: IntArray,
            updateWidget: (appWidgetIds: IntArray, views: RemoteViews) -> Unit
        ): ListenableWorker.Result {
            val ventas = entity?.ventasMontoTotal ?: 0.0
            val porCobrar = entity?.saldoPendienteTotal ?: 0.0

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_mi_negocio)
                views.setTextViewText(R.id.widget_hoy, "Hoy: ${ventas.formatAsCurrency()}")
                views.setTextViewText(R.id.widget_por_cobrar, "Por cobrar: ${porCobrar.formatAsCurrency()}")

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                updateWidget(intArrayOf(appWidgetId), views)
            }

            return ListenableWorker.Result.success()
        }

        private fun getAppWidgetManager(context: Context): AppWidgetManager =
            AppWidgetManager.getInstance(context)

        fun readOpticaId(context: Context): String = try {
            val prefs = context.getSharedPreferences("secure_session_prefs", Context.MODE_PRIVATE)
            prefs.getString("saas_optica_id", null) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
