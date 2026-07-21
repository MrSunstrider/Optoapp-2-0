package com.example.optoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.optoapp.MainActivity
import com.example.optoapp.R
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.util.formatAsCurrency
import com.example.optoapp.widget.MiNegocioWidgetWorker.Companion.readOpticaId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint(AppWidgetProvider::class)
class MiNegocioWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var dao: ResumenDiarioDao

    @Inject
    lateinit var sessionManager: SessionManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        scope.launch {
            val today = LocalDate.now().toString()
            val opticaId = readOpticaId(context)

            val entity = if (opticaId.isNotBlank()) {
                try {
                    dao.getByOpticaAndDate(opticaId, today)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

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
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scope.cancel()
    }
}
