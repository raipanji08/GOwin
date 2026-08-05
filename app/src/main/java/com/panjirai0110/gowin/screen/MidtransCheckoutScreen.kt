package com.panjirai0110.gowin.screen

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.panjirai0110.gowin.BuildConfig
import com.panjirai0110.gowin.payment.MidtransCheckout
import com.panjirai0110.gowin.ui.component.GowinActionCard
import com.panjirai0110.gowin.ui.component.GowinPrimaryActionButton
import com.panjirai0110.gowin.ui.component.GowinSecondaryActionButton
import com.panjirai0110.gowin.util.generateQR
import com.panjirai0110.gowin.viewmodel.MainViewModel
import com.panjirai0110.shared.model.Booking
import com.panjirai0110.shared.model.BookingStatus
import com.panjirai0110.shared.model.PaymentMethod
import com.panjirai0110.shared.ui.theme.GowinBlue
import com.panjirai0110.shared.ui.theme.GowinBorder
import com.panjirai0110.shared.ui.theme.GowinDark
import com.panjirai0110.shared.ui.theme.GowinGray
import com.panjirai0110.shared.ui.theme.GowinGreen
import com.panjirai0110.shared.ui.theme.GowinLightBlue
import com.panjirai0110.shared.ui.theme.GowinRed
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONTokener

@Composable
fun MidtransCheckoutScreen(
    bookingId: String,
    viewModel: MainViewModel,
    onPaymentFinished: () -> Unit
) {
    var leavingCheckout by rememberSaveable(bookingId) { mutableStateOf(false) }
    val exitGate = remember(bookingId) { AtomicBoolean(false) }
    val latestOnPaymentFinished by rememberUpdatedState(onPaymentFinished)
    val leaveCheckout = {
        if (exitGate.compareAndSet(false, true)) {
            leavingCheckout = true
            latestOnPaymentFinished()
        }
    }

    BackHandler(onBack = leaveCheckout)

    DisposableEffect(bookingId) {
        val observationGeneration = viewModel.observeBooking(bookingId)
        onDispose {
            viewModel.stopObservingBooking(observationGeneration)
        }
    }

    val booking = viewModel.currentBooking
    val checkout = viewModel.currentMidtransCheckout
        ?: booking?.let(MidtransCheckout::fromBooking)

    LaunchedEffect(booking?.status, leavingCheckout) {
        if (booking == null || leavingCheckout) return@LaunchedEffect
        if (booking.status != BookingStatus.PendingPayment) {
            leaveCheckout()
        }
    }

    val isSnapHosted = checkout?.deeplinkUrl?.startsWith(
        "https://app.sandbox.midtrans.com/",
        ignoreCase = true
    ) == true
    val onRefresh = {
        viewModel.refreshMidtransStatus(bookingId) { status ->
            if (status.bookingStatus != BookingStatus.PendingPayment) {
                leaveCheckout()
            }
        }
    }

    when {
        checkout != null && isSnapHosted -> SnapHostedCheckoutScreen(
            checkout = checkout,
            refreshing = viewModel.paymentStatusRefreshing,
            error = viewModel.paymentError,
            statusMessage = viewModel.paymentStatusMessage,
            onBack = leaveCheckout,
            onRefresh = onRefresh
        )

        booking == null || checkout == null -> PaymentLoadingState(
            error = viewModel.paymentError,
            onBack = leaveCheckout
        )

        else -> {
            MidtransCheckoutContent(
                booking = booking,
                checkout = checkout,
                refreshing = viewModel.paymentStatusRefreshing,
                error = viewModel.paymentError,
                statusMessage = viewModel.paymentStatusMessage,
                onBack = leaveCheckout,
                onRefresh = onRefresh
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapHostedCheckoutScreen(
    checkout: MidtransCheckout,
    refreshing: Boolean,
    error: String?,
    statusMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var pagePrepared by rememberSaveable(checkout.deeplinkUrl) {
        mutableStateOf(false)
    }
    val latestOnRefresh by rememberUpdatedState(onRefresh)
    val backendUri = remember {
        Uri.parse(BuildConfig.MIDTRANS_BACKEND_URL)
    }
    val webView = remember(checkout.deeplinkUrl) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)
            alpha = 0f
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.mixedContentMode =
                android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(
                this,
                true
            )
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                private var initialSnapPagePrepared = false

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val target = request.url
                    if (target.scheme.equals("https", ignoreCase = true)) {
                        return false
                    }
                    return runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, target)
                        )
                        true
                    }.getOrDefault(true)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    val target = Uri.parse(url)
                    if (target.isSnapHostedCheckout()) {
                        view.applyGowinSnapPresentation {
                            if (!initialSnapPagePrepared) {
                                initialSnapPagePrepared = true
                                view.alpha = 1f
                                pagePrepared = true
                            }
                        }
                    } else if (!initialSnapPagePrepared) {
                        initialSnapPagePrepared = true
                        view.alpha = 1f
                        pagePrepared = true
                    }
                    val reachedFinishPage =
                        target.scheme.equals(
                            backendUri.scheme,
                            ignoreCase = true
                        ) &&
                            target.host.equals(
                                backendUri.host,
                                ignoreCase = true
                            ) &&
                            target.path == "/finish"
                    if (reachedFinishPage) {
                        latestOnRefresh()
                    }
                }
            }
            loadUrl(checkout.deeplinkUrl)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(28.dp),
                            tint = GowinDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White
            ) {
                GowinActionCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                    error?.let {
                        Text(
                            text = it,
                            color = GowinRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    statusMessage?.let {
                        Text(
                            text = it,
                            color = GowinGreen,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (checkout.paymentMethod == PaymentMethod.MidtransQris) {
                        GowinSecondaryActionButton(
                            text = "UNDUH QRIS",
                            icon = Icons.Default.Download,
                            onClick = {
                                webView.resolveSnapQrisUrl { qrisUri ->
                                    val downloaded = qrisUri?.let { uri ->
                                        context.enqueueSnapQrisDownload(
                                            uri = uri,
                                            bookingCode = checkout.bookingCode,
                                            cookie = CookieManager.getInstance()
                                                .getCookie(uri.toString())
                                        )
                                    } == true
                                    Toast.makeText(
                                        context,
                                        if (downloaded) {
                                            "QRIS sedang diunduh."
                                        } else {
                                            "QRIS belum siap diunduh. Coba lagi sebentar."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    GowinPrimaryActionButton(
                        text = "CEK STATUS PEMBAYARAN",
                        loading = refreshing,
                        loadingText = "MEMERIKSA STATUS...",
                        onClick = onRefresh
                    )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
            if (!pagePrepared) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GowinBlue)
                }
            }
        }
    }
}

/**
 * GO-WIN owns the fixed payment actions below the checkout page. Snap also
 * renders duplicate status and QRIS-download actions, so hide only those actions.
 * Payment actions, payment instructions, and all non-Snap pages are untouched.
 */
private fun WebView.applyGowinSnapPresentation(onApplied: () -> Unit) {
    evaluateJavascript(HIDE_DUPLICATE_SNAP_ACTIONS) {
        onApplied()
    }
}

private fun WebView.resolveSnapQrisUrl(onResolved: (Uri?) -> Unit) {
    evaluateJavascript(RESOLVE_SNAP_QRIS_URL) { result ->
        val rawUrl = runCatching {
            JSONTokener(result).nextValue() as? String
        }.getOrNull()
        onResolved(
            rawUrl
                ?.let(Uri::parse)
                ?.takeIf(Uri::isMidtransSandboxAsset)
        )
    }
}

private fun Context.enqueueSnapQrisDownload(
    uri: Uri,
    bookingCode: String,
    cookie: String?
): Boolean = runCatching {
    val request = DownloadManager.Request(uri)
        .setTitle("GO-WIN QRIS $bookingCode")
        .setDescription("Menyimpan QRIS pembayaran GO-WIN")
        .setMimeType("image/png")
        .setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalFilesDir(
            this,
            Environment.DIRECTORY_DOWNLOADS,
            "GO-WIN-QRIS-$bookingCode.png"
        )
    cookie?.takeIf(String::isNotBlank)?.let { value ->
        request.addRequestHeader("Cookie", value)
    }
    getSystemService(DownloadManager::class.java).enqueue(request)
}.isSuccess

private fun Uri.isSnapHostedCheckout(): Boolean =
    scheme.equals("https", ignoreCase = true) &&
        host.equals("app.sandbox.midtrans.com", ignoreCase = true)

private fun Uri.isMidtransSandboxAsset(): Boolean =
    scheme.equals("https", ignoreCase = true) &&
        host?.lowercase()?.endsWith(".midtrans.com") == true

private const val HIDE_DUPLICATE_SNAP_ACTIONS =
    """
    (function() {
      var labels = ["check status", "cek status", "download qris", "unduh qris"];
      var normalize = function(value) {
        return String(value || "").replace(/\\s+/g, " ").trim().toLowerCase();
      };
      var hideDuplicateActions = function() {
        var nodes = document.querySelectorAll("button, [role='button'], a");
        nodes.forEach(function(node) {
          var label = normalize(node.innerText || node.textContent || node.getAttribute("aria-label"));
          if (labels.indexOf(label) !== -1) {
            node.style.setProperty("position", "absolute", "important");
            node.style.setProperty("width", "1px", "important");
            node.style.setProperty("height", "1px", "important");
            node.style.setProperty("opacity", "0", "important");
            node.style.setProperty("overflow", "hidden", "important");
            node.style.setProperty("pointer-events", "none", "important");
            node.setAttribute("aria-hidden", "true");
          }
        });
      };
      hideDuplicateActions();
      if (!window.__gowinSnapActionObserverInstalled) {
        window.__gowinSnapActionObserverInstalled = true;
        new MutationObserver(hideDuplicateActions).observe(document.documentElement, {
          childList: true,
          subtree: true
        });
      }
    })();
    """

private const val RESOLVE_SNAP_QRIS_URL =
    """
    (function() {
      var images = document.querySelectorAll("img[src]");
      for (var index = 0; index < images.length; index += 1) {
        var image = images[index];
        var source = image.currentSrc || image.src || "";
        if ((image.alt || "").trim().toLowerCase() === "qr-code" && source) {
          return new URL(source, document.baseURI).href;
        }
      }
      for (var index = 0; index < images.length; index += 1) {
        var image = images[index];
        var source = image.currentSrc || image.src || "";
        if (/\/qr-code(?:[/?#]|$)/i.test(source)) {
          return new URL(source, document.baseURI).href;
        }
      }
      return null;
    })();
    """

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MidtransCheckoutContent(
    booking: Booking,
    checkout: MidtransCheckout,
    refreshing: Boolean,
    error: String?,
    statusMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val methodTitle = when (checkout.paymentMethod) {
        PaymentMethod.MidtransQris -> "Pembayaran QRIS"
        PaymentMethod.MidtransEWallet -> "Pembayaran GoPay"
        PaymentMethod.MidtransVirtualAccount -> "Virtual Account"
        else -> "Pembayaran"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GowinDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(28.dp),
                            tint = GowinDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GowinLightBlue,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(GowinBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Menunggu Pembayaran",
                            color = GowinDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Status diperbarui otomatis secara aman.",
                            color = GowinGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, GowinBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = methodTitle,
                        color = GowinDark,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Kode booking ${checkout.bookingCode}",
                        color = GowinGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GowinBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    when (checkout.paymentMethod) {
                        PaymentMethod.MidtransQris -> QrisInstructions(
                            checkout = checkout
                        )

                        PaymentMethod.MidtransEWallet -> GopayInstructions(
                            context = context,
                            checkout = checkout
                        )

                        PaymentMethod.MidtransVirtualAccount ->
                            VirtualAccountInstructions(
                                context = context,
                                checkout = checkout
                            )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = GowinBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pembayaran",
                            color = GowinDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatRupiah(booking.totalAmount),
                            color = GowinGreen,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentCountdown(expiresAt = checkout.expiresAt)

                    error?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = GowinRed,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    statusMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = GowinGreen,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GowinPrimaryActionButton(
                        text = "CEK STATUS PEMBAYARAN",
                        loading = refreshing,
                        loadingText = "MEMERIKSA STATUS...",
                        onClick = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
private fun QrisInstructions(checkout: MidtransCheckout) {
    val generatedQr = remember(checkout.qrString) {
        checkout.qrString.takeIf(String::isNotBlank)?.let(::generateQR)
    }
    val remoteQr by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        checkout.qrUrl
    ) {
        if (generatedQr == null && checkout.qrUrl.isNotBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    URL(checkout.qrUrl).openStream().use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }
    val qrBitmap = generatedQr ?: remoteQr
    Box(
        modifier = Modifier
            .size(204.dp)
            .background(Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QRIS pembayaran",
                modifier = Modifier.size(188.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = GowinGray,
                modifier = Modifier.size(72.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Scan QR dengan aplikasi pembayaran yang mendukung QRIS.",
        color = GowinGray,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GopayInstructions(
    context: Context,
    checkout: MidtransCheckout
) {
    PaymentMethodIcon(
        icon = Icons.Default.Wallet,
        contentDescription = "GoPay"
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Selesaikan transaksi melalui aplikasi GoPay.",
        color = GowinGray,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(checkout.deeplinkUrl))
                )
            }.onFailure {
                Toast.makeText(
                    context,
                    "Aplikasi GoPay tidak dapat dibuka.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        enabled = checkout.deeplinkUrl.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GowinGreen,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "BUKA GOPAY",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VirtualAccountInstructions(
    context: Context,
    checkout: MidtransCheckout
) {
    PaymentMethodIcon(
        icon = Icons.Default.AccountBalance,
        contentDescription = "Virtual Account"
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Nomor Virtual Account ${checkout.virtualAccountBank.uppercase()}",
        color = GowinGray,
        fontSize = 12.sp
    )
    Spacer(modifier = Modifier.height(5.dp))
    Text(
        text = checkout.virtualAccountNumber,
        color = GowinDark,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Nomor Virtual Account",
                    checkout.virtualAccountNumber
                )
            )
            Toast.makeText(
                context,
                "Nomor Virtual Account disalin.",
                Toast.LENGTH_SHORT
            ).show()
        },
        modifier = Modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GowinBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = GowinBlue
        )
    ) {
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Salin Nomor VA",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PaymentMethodIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .background(GowinLightBlue, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GowinBlue,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun PaymentCountdown(expiresAt: Long) {
    var remainingMillis by remember(expiresAt) {
        mutableLongStateOf((expiresAt - System.currentTimeMillis()).coerceAtLeast(0L))
    }
    LaunchedEffect(expiresAt) {
        while (remainingMillis > 0L) {
            delay(1_000)
            remainingMillis =
                (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }
    val totalSeconds = remainingMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    Text(
        text = if (remainingMillis > 0L) {
            "Selesaikan dalam %02d:%02d".format(minutes, seconds)
        } else {
            "Waktu pembayaran telah berakhir"
        },
        color = if (remainingMillis > 0L) GowinGray else GowinRed,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentLoadingState(
    error: String?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Pembayaran",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (error == null) {
                CircularProgressIndicator(color = GowinBlue)
            } else {
                Text(
                    text = error,
                    color = GowinRed,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
