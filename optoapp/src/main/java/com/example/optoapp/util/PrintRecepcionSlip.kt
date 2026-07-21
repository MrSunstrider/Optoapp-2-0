package com.example.optoapp.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import java.time.format.DateTimeFormatter

/**
 * Generates and prints an HTML receipt slip for a completed purchase order.
 */
object PrintRecepcionSlip {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun print(context: Context, oc: OrdenCompra, items: List<OrdenCompraItem>, proveedorNombre: String = "") {
        val html = buildReceiptHtml(oc, items, proveedorNombre)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Recepcion OC ${oc.numero}"
                val adapter = view.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    adapter,
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build(),
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun buildReceiptHtml(oc: OrdenCompra, items: List<OrdenCompraItem>, proveedorNombre: String): String {
        val itemRows = items.joinToString("\n") { item ->
            """
                <tr>
                    <td>${item.monturaId}</td>
                    <td style="text-align:right">${item.cantidad}</td>
                    <td style="text-align:right">${String.format("%.2f", item.costoUnitario)}</td>
                    <td style="text-align:right">${String.format("%.2f", item.cantidad * item.costoUnitario)}</td>
                </tr>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: sans-serif; font-size: 12pt; padding: 2cm; }
                    h1 { text-align: center; font-size: 14pt; }
                    .header { margin-bottom: 16px; }
                    .header p { margin: 2px 0; }
                    table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                    th, td { border: 1px solid #000; padding: 4px 8px; }
                    th { background: #eee; text-align: left; }
                    .total { font-weight: bold; margin-top: 16px; text-align: right; }
                </style>
            </head>
            <body>
                <h1>Comprobante de Recepción</h1>
                <div class="header">
                    <p><strong>OC:</strong> ${oc.numero}</p>
                    <p><strong>Proveedor:</strong> $proveedorNombre</p>
                    <p><strong>Fecha:</strong> ${oc.fecha.format(dateFormatter)}</p>
                    <p><strong>Estado:</strong> ${oc.estado}</p>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th>Montura ID</th>
                            <th style="text-align:right">Cantidad</th>
                            <th style="text-align:right">Costo Unit.</th>
                            <th style="text-align:right">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemRows
                    </tbody>
                </table>
                <p class="total">Total: S/. ${String.format("%.2f", oc.total)}</p>
            </body>
            </html>
        """.trimIndent()
    }
}
