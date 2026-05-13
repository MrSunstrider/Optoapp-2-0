package com.example.optoapp.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.optoapp.subscription.SubscriptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * P2-T2: cliente Google Play Billing (suscripción). Crea el producto `SUBSCRIPTION_PRODUCT_ID` en Play Console.
 */
@Deprecated("Sin uso en alpha. Mantener por posible reactivación.")
@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext private val app: Context,
    private val subscriptionManager: SubscriptionManager
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var client: BillingClient? = null

    companion object {
        const val SUBSCRIPTION_PRODUCT_ID = "optoapp_pro_monthly"
    }

    private fun ensureClient(): BillingClient {
        client?.let { return it }
        val c = BillingClient.newBuilder(app)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            acknowledgeAndGrant(purchase)
                        }
                    }
                }
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = c
        return c
    }

    private fun acknowledgeAndGrant(purchase: Purchase) {
        ioScope.launch {
            val c = client ?: return@launch
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                c.acknowledgePurchase(params) { }
            }
            subscriptionManager.setProFromLocalCache()
        }
    }

    /**
     * Conecta a Play Store y ejecuta [block] con el cliente listo (o null si falla).
     */
    fun startConnection(onReady: (BillingClient) -> Unit, onFailure: () -> Unit) {
        val c = ensureClient()
        if (c.isReady) {
            onReady(c)
            return
        }
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady(c)
                } else {
                    onFailure()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private suspend fun querySubscriptionProduct(billingClient: BillingClient): ProductDetails? =
        suspendCancellableCoroutine { cont ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUBSCRIPTION_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            billingClient.queryProductDetailsAsync(params) { result, detailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK || detailsList.isNullOrEmpty()) {
                    cont.resume(null)
                } else {
                    cont.resume(detailsList.first())
                }
            }
        }

    /**
     * Lanza el flujo de compra. El producto debe existir en Play Console (prueba interna/cerrada).
     */
    fun launchSubscriptionPurchase(activity: Activity, onError: (String) -> Unit) {
        startConnection(
            onReady = { c ->
                ioScope.launch {
                    val details = querySubscriptionProduct(c)
                    if (details == null) {
                        onError("Producto no configurado en Play Console ($SUBSCRIPTION_PRODUCT_ID).")
                        return@launch
                    }
                    val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    if (offerToken.isNullOrBlank()) {
                        onError("La suscripción no tiene oferta activa en Play Console.")
                        return@launch
                    }
                    val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productParams))
                        .build()
                    withContext(Dispatchers.Main) {
                        val br = c.launchBillingFlow(activity, flowParams)
                        if (br.responseCode != BillingClient.BillingResponseCode.OK) {
                            onError("No se pudo abrir la compra (${br.responseCode}).")
                        }
                    }
                }
            },
            onFailure = { onError("Billing no disponible (¿Play Store instalado?).") }
        )
    }
}
