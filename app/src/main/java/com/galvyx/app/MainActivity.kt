package com.galvyx.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.galvyx.app.ui.theme.GalvyxAlienGreen
import com.galvyx.app.ui.theme.GalvyxCardElevated
import com.galvyx.app.ui.theme.GalvyxCyan
import com.galvyx.app.ui.theme.GalvyxTheme
import com.galvyx.app.ui.theme.GalvyxVioletBright
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "galvyx_local_store"
private const val PREF_VISITS = "visits"
private const val PREF_PROFILE = "profile"
private const val AUTO_BACKUP_FILE_NAME = "galvyx-auto-backup.json"

private val JOB_TYPES = listOf("General Service Call", "Initial MSP Assessment", "Network Survey", "Camera / Security", "Access Point / Wi-Fi", "Workstation Replacement", "Server / Firewall", "Low Voltage / Cabling", "Inspection", "Other")
private val CLIENT_TYPES = listOf("General Business", "Hotel / Hospitality", "Construction", "Restaurant / Bar", "Medical / Dental", "Retail", "Warehouse / Manufacturing", "Custom")
private val NOTE_CATEGORIES = listOf("General Note", "Network Port", "Switch / Firewall", "Access Point / Wi-Fi", "Camera", "Workstation", "Server", "Cable / Low Voltage", "Power", "PMS / Business App", "POS / Payments", "Door Locks / Access", "Line-of-Business App", "Compliance / Risk", "Expense / Receipt", "Issue Found", "Completed Work", "Follow-Up Needed")
private val DEVICE_TYPES = listOf("Firewall", "Switch", "Access Point", "Camera", "NVR / DVR", "Server", "Workstation", "Printer", "Payment Terminal", "Key Encoder", "Door Lock Controller", "Tablet / Mobile Device", "Plotter / Large Format Printer", "VoIP Phone", "Time Clock", "UPS / Battery Backup", "Other")
private val EXPENSE_CATEGORIES = listOf("Meal", "Gas", "Hotel", "Parking", "Tools / Supplies", "Equipment", "Mileage", "Toll", "Shipping", "Other")
private val PAYMENT_METHODS = listOf("Personal Card", "Company Card", "Cash", "Reimbursable", "Other")
private val PHOTO_CATEGORIES = listOf("General", "MDF", "IDF", "Rack", "Firewall", "Switch", "Access Point", "Camera", "NVR", "PMS Workstation", "POS Terminal", "Door Lock Encoder", "Printer", "Plotter", "Line-of-Business App", "Cabling", "Damage", "Receipt", "Before / After", "Other")
private val PHOTO_STAGES = listOf("Reference", "Before", "After", "Issue Found", "Completed Work", "Receipt")
private val EMAIL_PLATFORMS = listOf("Unknown", "Microsoft 365", "Google Workspace", "Hosted Exchange", "cPanel / Webmail", "GoDaddy", "Rackspace", "Other")
private val MFA_STATUSES = listOf("Unknown", "Enabled", "Partial", "Disabled", "Not Confirmed")
private val FILE_STORAGE_PLATFORMS = listOf("Unknown", "SharePoint / OneDrive", "Google Drive", "Dropbox", "Box", "Local File Server", "NAS", "Other")
private val BACKUP_PLATFORMS = listOf("Unknown", "Datto", "Veeam", "Acronis", "Windows Server Backup", "Synology / NAS", "Cloud Provider Snapshot", "Other")
private val SECURITY_PLATFORMS = listOf("Unknown", "Huntress", "Bitdefender", "Microsoft Defender", "SentinelOne", "CrowdStrike", "Sophos", "Other")
private val FIREWALL_PLATFORMS = listOf("Unknown", "SonicWall", "FortiGate", "UniFi Gateway", "Meraki", "pfSense", "WatchGuard", "Other")
private val WIFI_PLATFORMS = listOf("Unknown", "UniFi", "Aruba", "Meraki", "Ruckus", "TP-Link Omada", "Other")
private val RMM_PLATFORMS = listOf("Unknown", "Atera", "Datto RMM", "NinjaOne", "ConnectWise Automate", "Syncro", "None", "Other")

data class ClientAuditTemplate(
    val type: String,
    val summary: String,
    val coreSystems: List<String>,
    val keyQuestions: List<String>,
    val recommendedPhotos: List<String>
)

private val CLIENT_AUDIT_TEMPLATES = listOf(
    ClientAuditTemplate(
        type = "General Business",
        summary = "Baseline MSP audit for workstations, Microsoft 365, network, printers, backups, and security posture.",
        coreSystems = listOf("Microsoft 365 / Email", "Workstations", "Firewall / Switches / Wi-Fi", "Printers", "Backups", "Endpoint Security"),
        keyQuestions = listOf("Who owns admin access?", "Are MFA and endpoint security enabled?", "Where are important files stored?", "What breaks the business if it goes offline?"),
        recommendedPhotos = listOf("MDF / rack", "Firewall", "Switches", "Workstations", "Printers", "UPS")
    ),
    ClientAuditTemplate(
        type = "Hotel / Hospitality",
        summary = "Hotel audit focused on guest check-in, PMS, door locks, POS, cameras, guest Wi-Fi, and front desk continuity.",
        coreSystems = listOf("PMS / Property Management", "Door Locks / Key Encoders", "Restaurant POS", "Guest Wi-Fi", "Camera / NVR", "Front Desk Workstations", "Printers", "UniFi / Network"),
        keyQuestions = listOf("Can another device check guests in if the front desk PC fails?", "Does PMS integrate with door locks?", "Is the Visionline/database backup documented?", "Are POS, cameras, staff, and guest networks separated?", "Who has vendor support access?"),
        recommendedPhotos = listOf("Front desk PC", "Key encoder", "PMS/printer setup", "POS terminals", "NVR/camera status", "UniFi rack", "ISP/UPS")
    ),
    ClientAuditTemplate(
        type = "Construction",
        summary = "Construction audit for plan software, large files, plotters, SharePoint/project storage, remote access, and field devices.",
        coreSystems = listOf("Bluebeam", "AutoCAD / Revit", "Autodesk Construction Cloud / PlanGrid", "SharePoint / File Server", "Large Format Plotter", "VPN / Remote Access", "Field Tablets"),
        keyQuestions = listOf("How many Bluebeam/CAD users are licensed?", "Where are project drawings stored?", "Are large PDFs slow to open?", "Is plotter scanning/printing reliable?", "Do field users need tablet access?"),
        recommendedPhotos = listOf("CAD workstations", "Plotter model/serial", "Network settings page", "Jobsite Wi-Fi gear", "File storage screen", "VPN/network rack")
    ),
    ClientAuditTemplate(
        type = "Restaurant / Bar",
        summary = "Restaurant audit for POS uptime, kitchen printers/KDS, payment terminals, guest Wi-Fi, cameras, music/TV, and network segmentation.",
        coreSystems = listOf("Toast / POS", "Payment Terminals", "Kitchen Printers / KDS", "Guest Wi-Fi", "Cameras", "Music / TV / AV", "Firewall / Switches"),
        keyQuestions = listOf("Is POS on a dedicated network?", "Are kitchen printers static/reserved?", "Who calls POS vendor support?", "Is internet failover required?", "Are payment support boundaries clear?"),
        recommendedPhotos = listOf("POS terminals", "Kitchen printers", "KDS screens", "Payment devices", "Network rack", "Camera/NVR", "ISP gear")
    ),
    ClientAuditTemplate(
        type = "Medical / Dental",
        summary = "Medical/dental audit for practice software, imaging devices, HIPAA risk, backups, workstations by room, and vendor boundaries.",
        coreSystems = listOf("Practice Management Software", "Imaging / X-ray", "M365 / Email", "Backups", "HIPAA Security", "Room Workstations", "Printers / Scanners"),
        keyQuestions = listOf("What systems store patient data?", "Are backups tested and documented?", "Who supports imaging hardware/software?", "Are shared logins avoided?", "Is MFA enabled for cloud access?"),
        recommendedPhotos = listOf("Server/NAS", "Operatories/workstations", "Imaging workstation", "Network rack", "Backup device", "Printers/scanners")
    ),
    ClientAuditTemplate(
        type = "Retail",
        summary = "Retail audit for POS, inventory, barcode scanners, receipt printers, cameras, Wi-Fi, and payment network boundaries.",
        coreSystems = listOf("POS", "Inventory System", "Barcode Scanners", "Receipt Printers", "Payment Terminals", "Cameras", "Guest Wi-Fi"),
        keyQuestions = listOf("What happens if POS/internet fails?", "Are payment devices segmented?", "Are scanners/printers documented?", "Who owns vendor admin access?"),
        recommendedPhotos = listOf("POS station", "Payment terminal", "Receipt printer", "Scanner", "Network gear", "Cameras")
    ),
    ClientAuditTemplate(
        type = "Warehouse / Manufacturing",
        summary = "Warehouse/manufacturing audit for Wi-Fi coverage, barcode/RF devices, inventory/ERP systems, rugged devices, cameras, and uptime risks.",
        coreSystems = listOf("ERP / Inventory", "Barcode / RF Scanners", "Warehouse Wi-Fi", "Label Printers", "Rugged Workstations", "Cameras", "Network / UPS"),
        keyQuestions = listOf("Where are Wi-Fi dead spots?", "Which devices stop shipping/receiving?", "Are label printers static/reserved?", "Is network gear on UPS?", "Who supports ERP/inventory software?"),
        recommendedPhotos = listOf("Warehouse APs", "Scanner docks", "Label printers", "Shipping stations", "Network rack", "UPS", "Cameras")
    ),
    ClientAuditTemplate(
        type = "Custom",
        summary = "Custom template for niche clients. Use notes and photos to define line-of-business systems, risk, and vendor boundaries.",
        coreSystems = listOf("Line-of-Business App", "Network", "Workstations", "Printers", "Backups", "Vendor Support"),
        keyQuestions = listOf("What software runs the business?", "Who supports it?", "Where is data stored?", "What needs a backup/recovery plan?"),
        recommendedPhotos = listOf("Critical workstation", "Application/version screen", "Network gear", "Printers", "Vendor labels")
    )
)

private fun auditTemplateFor(clientType: String): ClientAuditTemplate = CLIENT_AUDIT_TEMPLATES.firstOrNull { it.type == clientType } ?: CLIENT_AUDIT_TEMPLATES.first()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalvyxTheme {
                GalvyxApp(context = this)
            }
        }
    }
}

private enum class Screen { Home, NewVisit, Recent, VisitDetail, Settings }
private enum class DialogKind { None, Note, Device, Expense, PhotoCaption, EditVisit, EditCoreSystems, EditNote, EditDevice, EditExpense, EditPhoto, ExportOptions }
private enum class DeleteKind { Visit, Note, Device, Expense, Photo }
private data class DeleteRequest(val kind: DeleteKind, val id: String, val title: String)
private data class ExportedReport(val uri: Uri, val displayName: String)
private data class ReportTarget(val uri: Uri, val displayName: String, val outputStream: OutputStream)
data class PdfExportOptions(
    val includeNotes: Boolean = true,
    val includeDevices: Boolean = true,
    val includeExpenses: Boolean = true,
    val includeExpenseReceipts: Boolean = true,
    val includePhotos: Boolean = true,
    val exportMode: String = "Email-Friendly PDF",
    val photoQuality: String = "Medium",
    val photosToInclude: String = "Key Photos Only"
) {
    val keyPhotosOnly: Boolean
        get() = photosToInclude == "Key Photos Only"

    val maxPdfImageDimension: Int
        get() = when (photoQuality) {
            "Small" -> 1024
            "Original / Archive" -> 2400
            else -> 1600
        }

    val jpegQuality: Int
        get() = when (photoQuality) {
            "Small" -> 65
            "Original / Archive" -> 92
            else -> 78
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalvyxApp(context: Context) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val visits = remember { mutableStateListOf<SiteVisit>().also { it.addAll(visitsFromJson(prefs.getString(PREF_VISITS, null))) } }
    var profile by remember {
        mutableStateOf(
            prefs.getString(PREF_PROFILE, null)?.let { raw ->
                runCatching { CompanyProfile.fromJson(org.json.JSONObject(raw)) }.getOrDefault(CompanyProfile())
            } ?: CompanyProfile()
        )
    }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var selectedVisitId by rememberSaveable { mutableStateOf<String?>(null) }
    var dialog by rememberSaveable { mutableStateOf(DialogKind.None) }
    var editingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteRequest by remember { mutableStateOf<DeleteRequest?>(null) }
    var isExportingPdf by remember { mutableStateOf(false) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExpenseReceiptId by rememberSaveable { mutableStateOf<String?>(null) }

    fun persistVisits() {
        prefs.edit().putString(PREF_VISITS, visitsToJson(visits)).apply()
        saveAutoBackup(context, profile, visits)
    }

    fun persistProfile(next: CompanyProfile) {
        profile = next
        prefs.edit().putString(PREF_PROFILE, next.toJson().toString()).apply()
        saveAutoBackup(context, next, visits)
    }

    fun upsertVisit(visit: SiteVisit) {
        val index = visits.indexOfFirst { it.id == visit.id }
        if (index >= 0) visits[index] = visit else visits.add(0, visit)
        persistVisits()
    }

    fun selectedVisit(): SiteVisit? = visits.firstOrNull { it.id == selectedVisitId }

    fun addReceiptPathToExpense(expenseId: String, receiptPath: String) {
        selectedVisit()?.let { current ->
            upsertVisit(
                current.copy(
                    expenses = current.expenses.map { expense ->
                        if (expense.id == expenseId) expense.copy(receiptPhotoPaths = expense.receiptPhotoPaths + receiptPath) else expense
                    }
                )
            )
        }
    }

    fun openVisit(visit: SiteVisit) {
        selectedVisitId = visit.id
        screen = Screen.VisitDetail
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val capturedPath = pendingPhotoPath
        val receiptExpenseId = pendingExpenseReceiptId
        if (success && capturedPath != null && receiptExpenseId != null) {
            addReceiptPathToExpense(receiptExpenseId, capturedPath)
            pendingPhotoPath = null
            pendingPhotoUri = null
            pendingExpenseReceiptId = null
            Toast.makeText(context, "Receipt scan attached", Toast.LENGTH_SHORT).show()
        } else if (success && capturedPath != null) {
            dialog = DialogKind.PhotoCaption
        } else {
            pendingPhotoPath = null
            pendingPhotoUri = null
            pendingExpenseReceiptId = null
        }
    }

    val documentScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val receiptExpenseId = pendingExpenseReceiptId
        if (result.resultCode == android.app.Activity.RESULT_OK && receiptExpenseId != null) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pageUri = scanResult?.pages?.firstOrNull()?.imageUri
            val savedPath = pageUri?.let { saveReceiptScanFromUri(context, profile, it) }
            if (savedPath != null) {
                addReceiptPathToExpense(receiptExpenseId, savedPath)
                Toast.makeText(context, "Enhanced receipt scan attached", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not save receipt scan", Toast.LENGTH_LONG).show()
            }
        }
        pendingPhotoPath = null
        pendingPhotoUri = null
        pendingExpenseReceiptId = null
    }

    fun launchExpenseCameraFallback(expenseId: String) {
        val created = createPhotoUri(context, profile, prefix = "receipt")
        if (created == null) {
            Toast.makeText(context, "Could not create receipt image", Toast.LENGTH_LONG).show()
        } else {
            pendingPhotoPath = created.first
            pendingPhotoUri = created.second
            pendingExpenseReceiptId = expenseId
            takePictureLauncher.launch(created.second)
        }
    }

    fun launchExpenseReceiptScan(expenseId: String) {
        val activity = context as? ComponentActivity
        if (activity == null) {
            launchExpenseCameraFallback(expenseId)
            return
        }
        pendingExpenseReceiptId = expenseId
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender ->
                documentScannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener {
                Toast.makeText(context, "Document scanner unavailable; opening camera", Toast.LENGTH_LONG).show()
                launchExpenseCameraFallback(expenseId)
            }
    }
    val reportsFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            persistProfile(profile.copy(localReportsTreeUri = uri.toString()))
            Toast.makeText(context, "Reports folder selected", Toast.LENGTH_SHORT).show()
        }
    }
    val photosFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            persistProfile(profile.copy(localPhotosTreeUri = uri.toString()))
            Toast.makeText(context, "Photos folder selected", Toast.LENGTH_SHORT).show()
        }
    }
    val backupFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val next = profile.copy(backupTreeUri = uri.toString(), autoBackupEnabled = true)
            persistProfile(next)
            val success = saveAutoBackup(context, next, visits)
            Toast.makeText(context, if (success) "Auto backup enabled" else "Backup folder selected, but first backup failed", Toast.LENGTH_LONG).show()
        }
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val success = exportBackupJson(context, uri, profile, visits)
            Toast.makeText(context, if (success) "Backup exported" else "Backup export failed", Toast.LENGTH_LONG).show()
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val backup = importBackupJson(context, uri)
            if (backup == null) {
                Toast.makeText(context, "Backup import failed", Toast.LENGTH_LONG).show()
            } else {
                visits.clear()
                visits.addAll(backup.visits)
                persistVisits()
                persistProfile(backup.profile.withClearedFolderPermissions())
                Toast.makeText(context, "Backup imported. Choose backup/photo folders again if needed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun navigateBackInApp() {
        when (screen) {
            Screen.Home -> Unit
            Screen.VisitDetail -> screen = Screen.Recent
            else -> screen = Screen.Home
        }
    }

    BackHandler(enabled = screen != Screen.Home) {
        navigateBackInApp()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (screen != Screen.Home) {
                TopAppBar(
                    title = { Text(screen.title()) },
                    navigationIcon = {
                        TextButton(onClick = { navigateBackInApp() }) {
                            Text("Back")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    visitCount = visits.size,
                    onNewVisit = { screen = Screen.NewVisit },
                    onRecent = { screen = Screen.Recent },
                    onSettings = { screen = Screen.Settings }
                )
                Screen.NewVisit -> NewVisitScreen(
                    defaultTechnician = profile.technicianName,
                    onSave = { visit -> upsertVisit(visit); openVisit(visit) }
                )
                Screen.Recent -> RecentVisitsScreen(
                    visits = visits,
                    onOpen = ::openVisit,
                    onDelete = { visit -> deleteRequest = DeleteRequest(DeleteKind.Visit, visit.id, visit.title) },
                    onNewVisit = { screen = Screen.NewVisit }
                )
                Screen.VisitDetail -> selectedVisit()?.let { visit ->
                    VisitDetailScreen(
                        visit = visit,
                        onAddNote = { dialog = DialogKind.Note },
                        onAddDevice = { dialog = DialogKind.Device },
                        onAddExpense = { dialog = DialogKind.Expense },
                        onEditVisit = { dialog = DialogKind.EditVisit },
                        onEditCoreSystems = { dialog = DialogKind.EditCoreSystems },
                        onAddPhoto = {
                            val created = createPhotoUri(context, profile)
                            if (created == null) {
                                Toast.makeText(context, "Could not create photo file", Toast.LENGTH_LONG).show()
                            } else {
                                pendingPhotoPath = created.first
                                pendingPhotoUri = created.second
                                takePictureLauncher.launch(created.second)
                            }
                        },
                        onEditNote = { note -> editingItemId = note.id; dialog = DialogKind.EditNote },
                        onEditDevice = { device -> editingItemId = device.id; dialog = DialogKind.EditDevice },
                        onEditExpense = { expense -> editingItemId = expense.id; dialog = DialogKind.EditExpense },
                        onScanExpenseReceipt = { expense -> launchExpenseReceiptScan(expense.id) },
                        onEditPhoto = { photo -> editingItemId = photo.id; dialog = DialogKind.EditPhoto },
                        onDeleteNote = { note -> deleteRequest = DeleteRequest(DeleteKind.Note, note.id, note.title.ifBlank { note.category }) },
                        onDeleteDevice = { device -> deleteRequest = DeleteRequest(DeleteKind.Device, device.id, device.hostname.ifBlank { device.deviceType }) },
                        onDeleteExpense = { expense -> deleteRequest = DeleteRequest(DeleteKind.Expense, expense.id, expense.vendor.ifBlank { expense.category }) },
                        onDeletePhoto = { photo -> deleteRequest = DeleteRequest(DeleteKind.Photo, photo.id, photo.caption.ifBlank { "Photo" }) },
                        onExport = { dialog = DialogKind.ExportOptions }
                    )
                } ?: EmptyState("Visit not found", "Go back and choose a recent site visit.")
                Screen.Settings -> SettingsScreen(
                    profile = profile,
                    onSave = ::persistProfile,
                    onChooseReportsFolder = { reportsFolderLauncher.launch(null) },
                    onClearReportsFolder = { persistProfile(profile.copy(localReportsTreeUri = "")) },
                    onChoosePhotosFolder = { photosFolderLauncher.launch(null) },
                    onClearPhotosFolder = { persistProfile(profile.copy(localPhotosTreeUri = "")) },
                    onChooseBackupFolder = { backupFolderLauncher.launch(null) },
                    onClearBackupFolder = { persistProfile(profile.copy(backupTreeUri = "", autoBackupEnabled = false)) },
                    onToggleAutoBackup = { enabled -> persistProfile(profile.copy(autoBackupEnabled = enabled && profile.backupTreeUri.isNotBlank())) },
                    onExportBackup = { backupExportLauncher.launch(defaultBackupFileName()) },
                    onImportBackup = { backupImportLauncher.launch("application/json") }
                )
            }
        }
    }

    val visit = selectedVisit()
    if (visit != null) {
        when (dialog) {
            DialogKind.Note -> NoteDialog(
                onDismiss = { dialog = DialogKind.None },
                onSave = { note -> upsertVisit(visit.copy(notes = visit.notes + note)); dialog = DialogKind.None }
            )
            DialogKind.EditVisit -> VisitDialog(
                visit = visit,
                onDismiss = { dialog = DialogKind.None },
                onSave = { updated -> upsertVisit(updated); dialog = DialogKind.None }
            )
            DialogKind.EditCoreSystems -> CoreSystemsDialog(
                coreSystems = visit.coreSystems,
                onDismiss = { dialog = DialogKind.None },
                onSave = { systems -> upsertVisit(visit.copy(coreSystems = systems)); dialog = DialogKind.None }
            )
            DialogKind.EditNote -> visit.notes.firstOrNull { it.id == editingItemId }?.let { existing ->
                NoteDialog(
                    existing = existing,
                    onDismiss = { editingItemId = null; dialog = DialogKind.None },
                    onSave = { note -> upsertVisit(visit.copy(notes = visit.notes.map { if (it.id == note.id) note else it })); editingItemId = null; dialog = DialogKind.None }
                )
            }
            DialogKind.Device -> DeviceDialog(
                onDismiss = { dialog = DialogKind.None },
                onSave = { device -> upsertVisit(visit.copy(devices = visit.devices + device)); dialog = DialogKind.None }
            )
            DialogKind.EditDevice -> visit.devices.firstOrNull { it.id == editingItemId }?.let { existing ->
                DeviceDialog(
                    existing = existing,
                    onDismiss = { editingItemId = null; dialog = DialogKind.None },
                    onSave = { device -> upsertVisit(visit.copy(devices = visit.devices.map { if (it.id == device.id) device else it })); editingItemId = null; dialog = DialogKind.None }
                )
            }
            DialogKind.Expense -> ExpenseDialog(
                onDismiss = { dialog = DialogKind.None },
                onSave = { expense -> upsertVisit(visit.copy(expenses = visit.expenses + expense)); dialog = DialogKind.None },
                onSaveAndScanReceipt = { expense ->
                    upsertVisit(visit.copy(expenses = visit.expenses + expense))
                    dialog = DialogKind.None
                    launchExpenseReceiptScan(expense.id)
                }
            )
            DialogKind.EditExpense -> visit.expenses.firstOrNull { it.id == editingItemId }?.let { existing ->
                ExpenseDialog(
                    existing = existing,
                    onDismiss = { editingItemId = null; dialog = DialogKind.None },
                    onSave = { expense -> upsertVisit(visit.copy(expenses = visit.expenses.map { if (it.id == expense.id) expense else it })); editingItemId = null; dialog = DialogKind.None },
                    onSaveAndScanReceipt = { expense ->
                        upsertVisit(visit.copy(expenses = visit.expenses.map { if (it.id == expense.id) expense else it }))
                        editingItemId = null
                        dialog = DialogKind.None
                        launchExpenseReceiptScan(expense.id)
                    },
                    onDeleteReceipt = { expenseId, receiptPath ->
                        deleteAppOwnedPhoto(context, receiptPath, profile)
                        selectedVisit()?.let { current ->
                            upsertVisit(
                                current.copy(
                                    expenses = current.expenses.map { expense ->
                                        if (expense.id == expenseId) expense.copy(receiptPhotoPaths = expense.receiptPhotoPaths.filterNot { it == receiptPath }) else expense
                                    }
                                )
                            )
                        }
                        Toast.makeText(context, "Receipt removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            DialogKind.EditPhoto -> visit.photos.firstOrNull { it.id == editingItemId }?.let { existing ->
                PhotoCaptionDialog(
                    existing = existing,
                    onDismiss = { editingItemId = null; dialog = DialogKind.None },
                    onSave = { photo -> upsertVisit(visit.copy(photos = visit.photos.map { if (it.id == photo.id) photo else it })); editingItemId = null; dialog = DialogKind.None }
                )
            }
            DialogKind.PhotoCaption -> PhotoCaptionDialog(
                onDismiss = { pendingPhotoPath = null; pendingPhotoUri = null; dialog = DialogKind.None },
                onSave = { photo ->
                    val path = pendingPhotoPath
                    if (path != null) upsertVisit(visit.copy(photos = visit.photos + photo.copy(path = path)))
                    pendingPhotoPath = null
                    pendingPhotoUri = null
                    dialog = DialogKind.None
                }
            )
            DialogKind.ExportOptions -> PdfExportOptionsDialog(
                visit = visit,
                isExporting = isExportingPdf,
                onDismiss = { dialog = DialogKind.None },
                onExport = { options ->
                    if (!isExportingPdf) {
                        isExportingPdf = true
                        val visitSnapshot = visit
                        val profileSnapshot = profile
                        coroutineScope.launch {
                            val report = withContext(Dispatchers.IO) {
                                exportVisitPdf(context.applicationContext, visitSnapshot, profileSnapshot, options)
                            }
                            isExportingPdf = false
                            dialog = DialogKind.None
                            if (report != null) shareReport(context, report) else Toast.makeText(context, "PDF export failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
            DialogKind.None -> Unit
        }
    }

    deleteRequest?.let { request ->
        ConfirmDeleteDialog(
            request = request,
            onDismiss = { deleteRequest = null },
            onConfirm = {
                when (request.kind) {
                    DeleteKind.Visit -> {
                        visits.removeAll { it.id == request.id }
                        if (selectedVisitId == request.id) { selectedVisitId = null; screen = Screen.Recent }
                    }
                    DeleteKind.Note -> selectedVisit()?.let { current -> upsertVisit(current.copy(notes = current.notes.filterNot { it.id == request.id })) }
                    DeleteKind.Device -> selectedVisit()?.let { current -> upsertVisit(current.copy(devices = current.devices.filterNot { it.id == request.id })) }
                    DeleteKind.Expense -> selectedVisit()?.let { current ->
                        current.expenses.firstOrNull { it.id == request.id }?.receiptPhotoPaths?.forEach { deleteAppOwnedPhoto(context, it, profile) }
                        upsertVisit(current.copy(expenses = current.expenses.filterNot { it.id == request.id }))
                    }
                    DeleteKind.Photo -> selectedVisit()?.let { current ->
                        current.photos.firstOrNull { it.id == request.id }?.let { deleteAppOwnedPhoto(context, it.path, profile) }
                        upsertVisit(current.copy(photos = current.photos.filterNot { it.id == request.id }))
                    }
                }
                persistVisits()
                deleteRequest = null
                Toast.makeText(context, "Deleted ${request.kind.name.lowercase(Locale.US)}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun Screen.title(): String = when (this) {
    Screen.Home -> "Galvyx"
    Screen.NewVisit -> "New Site Visit"
    Screen.Recent -> "Recent Site Visits"
    Screen.VisitDetail -> "Visit Details"
    Screen.Settings -> "Settings"
}

@Composable
fun HomeScreen(
    visitCount: Int = 0,
    onNewVisit: () -> Unit = {},
    onRecent: () -> Unit = {},
    onSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ConstellationBackdrop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GalvyxLogo()

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = GalvyxCardElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "FIELD OPS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GalvyxCyan,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Galvyx",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Field Notes, Photos & Reports",
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            CardPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ready for field work",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start a report in seconds.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "$visitCount saved",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                CapabilityStrip()

                Text(
                    text = "Capture notes, photos, device details, expenses, and follow-up items from the field.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                PrimaryAction("＋ New Site Visit", onClick = onNewVisit)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeSecondaryButton("◌ Recent", Modifier.weight(1f), onRecent)
                    HomeSecondaryButton("⚙ Settings", Modifier.weight(1f), onSettings)
                }

                Text(
                    text = "Export or share reports when the job is ready.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ConstellationBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val stars = listOf(
            Offset(size.width * 0.12f, size.height * 0.16f),
            Offset(size.width * 0.26f, size.height * 0.10f),
            Offset(size.width * 0.45f, size.height * 0.18f),
            Offset(size.width * 0.82f, size.height * 0.13f),
            Offset(size.width * 0.76f, size.height * 0.31f),
            Offset(size.width * 0.18f, size.height * 0.72f),
            Offset(size.width * 0.34f, size.height * 0.82f),
            Offset(size.width * 0.69f, size.height * 0.78f),
            Offset(size.width * 0.88f, size.height * 0.68f)
        )

        val links = listOf(0 to 1, 1 to 2, 3 to 4, 5 to 6, 6 to 7, 7 to 8)
        links.forEach { (start, end) ->
            drawLine(
                color = outline.copy(alpha = 0.18f),
                start = stars[start],
                end = stars[end],
                strokeWidth = 1.2f
            )
        }

        stars.forEachIndexed { index, point ->
            val color = if (index % 3 == 0) secondary else primary
            drawCircle(
                color = color.copy(alpha = if (index % 2 == 0) 0.45f else 0.26f),
                radius = if (index % 3 == 0) 3.2f else 2.0f,
                center = point
            )
        }

        drawCircle(
            color = secondary.copy(alpha = 0.08f),
            radius = size.minDimension * 0.36f,
            center = Offset(size.width * 0.5f, size.height * 0.52f)
        )
    }
}

@Composable
fun GalvyxLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.galvyx_mark),
        contentDescription = "Galvyx logo",
        modifier = modifier.size(104.dp),
        contentScale = ContentScale.Fit
    )
}


@Composable
fun TemplatePreviewCard(template: ClientAuditTemplate) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MSP Client Template", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(template.type, color = GalvyxCyan, fontWeight = FontWeight.SemiBold)
            }
            NeonPill("Audit Ready", GalvyxAlienGreen)
        }
        Text(template.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TemplateBulletGroup("Core systems", template.coreSystems.take(5))
        TemplateBulletGroup("Photos to capture", template.recommendedPhotos.take(4))
    }
}

@Composable
fun TemplateDetailCard(template: ClientAuditTemplate) {
    CardPanel {
        Text("${template.type} Audit Guide", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(template.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TemplateBulletGroup("Core systems to document", template.coreSystems)
        TemplateBulletGroup("Questions to answer", template.keyQuestions)
        TemplateBulletGroup("Recommended photo evidence", template.recommendedPhotos)
        HintText("Use Add Note for answers, Add Device for assets, and Add Photo with categories like PMS Workstation, POS Terminal, Door Lock Encoder, Plotter, NVR, MDF, and IDF.")
    }
}

@Composable
fun CoreSystemsCard(coreSystems: CoreSystems, onEdit: () -> Unit) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Core Systems", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Email, storage, security, firewall, Wi-Fi, RMM, and business platforms.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
        val rows = coreSystems.summaryRows
        if (rows.isEmpty()) {
            SmallEmpty("No core systems captured yet")
        } else {
            rows.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(label, color = GalvyxCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(0.8f))
                    Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                }
            }
        }
    }
}

@Composable
fun TemplateBulletGroup(title: String, rows: List<String>) {
    if (rows.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, color = GalvyxCyan, fontSize = 13.sp)
        rows.forEach { row -> Text("• $row", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
    }
}

@Composable
fun NewVisitScreen(defaultTechnician: String, onSave: (SiteVisit) -> Unit) {
    var client by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var tech by rememberSaveable { mutableStateOf(defaultTechnician) }
    var date by rememberSaveable { mutableStateOf(todayString()) }
    var jobType by rememberSaveable { mutableStateOf("Initial MSP Assessment") }
    var clientType by rememberSaveable { mutableStateOf("General Business") }
    val template = auditTemplateFor(clientType)

    FormColumn {
        Text("Create the shell first, then add notes/photos/devices/expenses from the visit page.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormTextField("Client / Site Name", client) { client = it }
        FormTextField("Project Name", project) { project = it }
        FormTextField("Technician Name", tech) { tech = it }
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Client Type", clientType, CLIENT_TYPES) { clientType = it }
        SimpleDropdown("Job Type", jobType, JOB_TYPES) { jobType = it }
        TemplatePreviewCard(template)
        PrimaryAction("Save Site Visit") {
            if (client.isBlank() && project.isBlank()) return@PrimaryAction
            onSave(SiteVisit(clientName = client.trim(), projectName = project.trim(), technicianName = tech.trim(), date = date.trim(), jobType = jobType.trim().ifBlank { "General Service Call" }, clientType = clientType.trim().ifBlank { "General Business" }))
        }
    }
}

@Composable
fun RecentVisitsScreen(visits: List<SiteVisit>, onOpen: (SiteVisit) -> Unit, onDelete: (SiteVisit) -> Unit, onNewVisit: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var jobFilter by rememberSaveable { mutableStateOf("All Job Types") }
    var clientTypeFilter by rememberSaveable { mutableStateOf("All Client Types") }
    val jobOptions = listOf("All Job Types") + JOB_TYPES
    val clientTypeOptions = listOf("All Client Types") + CLIENT_TYPES
    val filteredVisits = visits.filter { visit ->
        visit.matchesSearch(query) &&
            (jobFilter == "All Job Types" || visit.jobType == jobFilter) &&
            (clientTypeFilter == "All Client Types" || visit.clientTypeLabel == clientTypeFilter)
    }

    if (visits.isEmpty()) {
        EmptyState("No visits yet", "Create your first site visit and Galvyx will keep it locally on this device.")
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) { PrimaryAction("New Site Visit", onClick = onNewVisit) }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CardPanel {
                Text("Find a visit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                FormTextField("Search client, project, tech, notes, devices, expenses", query) { query = it }
                SimpleDropdown("Job Filter", jobFilter, jobOptions) { jobFilter = it }
                SimpleDropdown("Client Type Filter", clientTypeFilter, clientTypeOptions) { clientTypeFilter = it }
                Text("${filteredVisits.size} of ${visits.size} visits", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (filteredVisits.isEmpty()) {
            item { SmallEmpty("No visits match your search or filter") }
        }
        items(filteredVisits, key = { it.id }) { visit ->
            CardPanel {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(visit.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("${visit.date} • ${visit.technicianName.ifBlank { "Technician TBD" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NeonPill(visit.clientTypeLabel.take(18), GalvyxAlienGreen)
                        NeonPill(visit.jobType.take(18), GalvyxCyan)
                    }
                }
                VisitMetricRow(
                    notes = visit.notes.size,
                    photos = visit.photos.size,
                    devices = visit.devices.size,
                    expenses = visit.expenses.size,
                    compact = true
                )
                Text(visit.summary(), color = GalvyxCyan, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { onOpen(visit) }, modifier = Modifier.weight(1f)) { Text("Open") }
                    OutlinedButton(onClick = { onDelete(visit) }, modifier = Modifier.weight(1f)) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
fun VisitDetailScreen(
    visit: SiteVisit,
    onAddNote: () -> Unit,
    onAddDevice: () -> Unit,
    onAddExpense: () -> Unit,
    onAddPhoto: () -> Unit,
    onEditVisit: () -> Unit,
    onEditCoreSystems: () -> Unit,
    onEditNote: (VisitNote) -> Unit,
    onEditDevice: (DeviceInfo) -> Unit,
    onEditExpense: (VisitExpense) -> Unit,
    onScanExpenseReceipt: (VisitExpense) -> Unit,
    onEditPhoto: (VisitPhoto) -> Unit,
    onDeleteNote: (VisitNote) -> Unit,
    onDeleteDevice: (DeviceInfo) -> Unit,
    onDeleteExpense: (VisitExpense) -> Unit,
    onDeletePhoto: (VisitPhoto) -> Unit,
    onExport: () -> Unit
) {
    val expenseBreakdown = visit.expenseBreakdown
    val template = auditTemplateFor(visit.clientTypeLabel)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CardPanel {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(visit.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${visit.date} • ${visit.technicianName.ifBlank { "Technician not set" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NeonPill(visit.clientTypeLabel, GalvyxAlienGreen)
                        NeonPill(visit.jobType, GalvyxVioletBright)
                    }
                }
                Text(visit.summary(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                VisitMetricRow(
                    notes = visit.notes.size,
                    photos = visit.photos.size,
                    devices = visit.devices.size,
                    expenses = visit.expenses.size,
                    expenseTotal = expenseBreakdown.total
                )
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                TwoColumnActions(
                    "Add Note" to onAddNote,
                    "Add Photo" to onAddPhoto,
                    "Add Device" to onAddDevice,
                    "Add Expense" to onAddExpense,
                    "Core Systems" to onEditCoreSystems,
                    "Edit Visit" to onEditVisit
                )
                PrimaryAction("Export / Share PDF Report", onClick = onExport)
            }
        }
        item { TemplateDetailCard(template) }
        item { CoreSystemsCard(visit.coreSystems, onEdit = onEditCoreSystems) }
        if (visit.expenses.isNotEmpty()) item { ExpenseBreakdownCard(expenseBreakdown) }
        item { SectionHeader("Notes", visit.notes.size) }
        if (visit.notes.isEmpty()) item { SmallEmpty("No notes yet") }
        items(visit.notes, key = { it.id }) { note ->
            DetailCard(note.title.ifBlank { note.category }, "${note.location} • ${note.category}", note.notes, onEdit = { onEditNote(note) }, onDelete = { onDeleteNote(note) })
        }
        item { SectionHeader("Devices", visit.devices.size) }
        if (visit.devices.isEmpty()) item { SmallEmpty("No devices yet") }
        items(visit.devices, key = { it.id }) { device ->
            DetailCard(device.hostname.ifBlank { device.deviceType }, "${device.location} • ${device.manufacturer} ${device.model}", listOf(device.ipAddress, device.macAddress, device.serialNumber, device.notes).filter { it.isNotBlank() }.joinToString("\n"), onEdit = { onEditDevice(device) }, onDelete = { onDeleteDevice(device) })
        }
        item { SectionHeader("Expenses", visit.expenses.size) }
        if (visit.expenses.isEmpty()) item { SmallEmpty("No expenses yet") }
        items(visit.expenses, key = { it.id }) { expense ->
            ExpenseDetailCard(
                expense = expense,
                onScanReceipt = { onScanExpenseReceipt(expense) },
                onEdit = { onEditExpense(expense) },
                onDelete = { onDeleteExpense(expense) }
            )
        }
        item { SectionHeader("Photos", visit.photos.size) }
        if (visit.photos.isEmpty()) item { SmallEmpty("No photos yet") }
        items(visit.photos, key = { it.id }) { photo ->
            PhotoDetailCard(photo = photo, onEdit = { onEditPhoto(photo) }, onDelete = { onDeletePhoto(photo) })
        }
    }
}

@Composable
fun SettingsScreen(
    profile: CompanyProfile,
    onSave: (CompanyProfile) -> Unit,
    onChooseReportsFolder: () -> Unit,
    onClearReportsFolder: () -> Unit,
    onChoosePhotosFolder: () -> Unit,
    onClearPhotosFolder: () -> Unit,
    onChooseBackupFolder: () -> Unit,
    onClearBackupFolder: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    var company by rememberSaveable(profile.companyName) { mutableStateOf(profile.companyName) }
    var tech by rememberSaveable(profile.technicianName) { mutableStateOf(profile.technicianName) }
    var footer by rememberSaveable(profile.reportFooter) { mutableStateOf(profile.reportFooter) }
    var storageMode by rememberSaveable(profile.storageMode.name) { mutableStateOf(profile.storageMode.label) }
    var localRootFolder by rememberSaveable(profile.localRootFolder) { mutableStateOf(profile.localRootFolder) }
    var localReportsFolder by rememberSaveable(profile.localReportsFolder) { mutableStateOf(profile.localReportsFolder) }
    var localPhotosFolder by rememberSaveable(profile.localPhotosFolder) { mutableStateOf(profile.localPhotosFolder) }
    var sharePointSiteUrl by rememberSaveable(profile.sharePointSiteUrl) { mutableStateOf(profile.sharePointSiteUrl) }
    var sharePointLibrary by rememberSaveable(profile.sharePointLibraryName) { mutableStateOf(profile.sharePointLibraryName) }
    var sharePointFolder by rememberSaveable(profile.sharePointFolderPath) { mutableStateOf(profile.sharePointFolderPath) }

    FormColumn {
        Text("Company Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Set the name, report footer, and storage destination used for site visit reports.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormTextField("Company Name", company) { company = it }
        FormTextField("Default Technician Name", tech) { tech = it }
        FormTextField("Report Footer", footer) { footer = it }

        Spacer(Modifier.height(6.dp))
        Text("Storage", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Choose where Galvyx will save reports and attachments. Microsoft 365 / SharePoint sync is planned for a future release.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SimpleDropdown("Storage Destination", storageMode, listOf(StorageMode.Device.label, StorageMode.SharePoint.label)) { storageMode = it }

        if (StorageMode.fromLabel(storageMode) == StorageMode.Device) {
            FormTextField("Main Folder", localRootFolder) { localRootFolder = it }
            FormTextField("Reports Folder", localReportsFolder) { localReportsFolder = it }
            FormTextField("Photos Folder", localPhotosFolder) { localPhotosFolder = it }
            HintText("Default app storage path uses those folder names. To save to a specific folder on this phone, choose folders below.")
            FolderPickerRow(
                title = "PDF Report Folder",
                uri = profile.localReportsTreeUri,
                onChoose = onChooseReportsFolder,
                onClear = onClearReportsFolder
            )
            FolderPickerRow(
                title = "Photo Folder",
                uri = profile.localPhotosTreeUri,
                onChoose = onChoosePhotosFolder,
                onClear = onClearPhotosFolder
            )
        }

        Spacer(Modifier.height(6.dp))
        Text("Backup & Restore", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Recommended: choose a backup folder once. Galvyx will keep an automatic backup there after every saved change. After reinstalling, use Import Backup and choose galvyx-auto-backup.json.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FolderPickerRow(
            title = "Automatic Backup Folder",
            uri = profile.backupTreeUri,
            emptyText = "Auto backup is off",
            clearText = "Turn Off",
            onChoose = onChooseBackupFolder,
            onClear = onClearBackupFolder
        )
        if (profile.backupTreeUri.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onToggleAutoBackup(!profile.autoBackupEnabled) }, modifier = Modifier.weight(1f)) {
                    Text(if (profile.autoBackupEnabled) "Pause Auto Backup" else "Resume Auto Backup")
                }
                Text(
                    if (profile.autoBackupEnabled) "Auto backup: On" else "Auto backup: Paused",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
            }
        }
        HintText("Manual export is still available for one-off backups. Uninstalling Android apps deletes private app data, so keep backups/photos in a folder outside the app.")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("Export Backup") }
            OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("Import Backup") }
        }

        if (StorageMode.fromLabel(storageMode) == StorageMode.SharePoint) {
            FormTextField("SharePoint Site URL", sharePointSiteUrl) { sharePointSiteUrl = it }
            FormTextField("Document Library", sharePointLibrary) { sharePointLibrary = it }
            FormTextField("Folder Path", sharePointFolder) { sharePointFolder = it }
            HintText("Connection, Microsoft sign-in, and upload sync are not active yet. These fields reserve the configuration Galvyx will use when SharePoint support is added.")
        }

        PrimaryAction("Save Settings") {
            onSave(
                CompanyProfile(
                    companyName = company.trim(),
                    technicianName = tech.trim(),
                    reportFooter = footer.trim().ifBlank { "Generated by Galvyx" },
                    storageMode = StorageMode.fromLabel(storageMode),
                    localRootFolder = localRootFolder.trim().ifBlank { "Galvyx" },
                    localReportsFolder = localReportsFolder.trim().ifBlank { "Reports" },
                    localPhotosFolder = localPhotosFolder.trim().ifBlank { "Photos" },
                    localReportsTreeUri = profile.localReportsTreeUri,
                    localPhotosTreeUri = profile.localPhotosTreeUri,
                    backupTreeUri = profile.backupTreeUri,
                    autoBackupEnabled = profile.autoBackupEnabled,
                    sharePointSiteUrl = sharePointSiteUrl.trim(),
                    sharePointLibraryName = sharePointLibrary.trim().ifBlank { "Documents" },
                    sharePointFolderPath = sharePointFolder.trim().ifBlank { "Galvyx/Site Visits" }
                )
            )
        }
    }
}

@Composable
fun CoreSystemsDialog(coreSystems: CoreSystems, onDismiss: () -> Unit, onSave: (CoreSystems) -> Unit) {
    var emailPlatform by rememberSaveable { mutableStateOf(coreSystems.emailPlatform) }
    var emailDomain by rememberSaveable { mutableStateOf(coreSystems.emailDomain) }
    var adminPortal by rememberSaveable { mutableStateOf(coreSystems.adminPortal) }
    var mfaStatus by rememberSaveable { mutableStateOf(coreSystems.mfaStatus) }
    var fileStorage by rememberSaveable { mutableStateOf(coreSystems.fileStorage) }
    var backupPlatform by rememberSaveable { mutableStateOf(coreSystems.backupPlatform) }
    var securityPlatform by rememberSaveable { mutableStateOf(coreSystems.securityPlatform) }
    var firewallPlatform by rememberSaveable { mutableStateOf(coreSystems.firewallPlatform) }
    var wifiPlatform by rememberSaveable { mutableStateOf(coreSystems.wifiPlatform) }
    var rmmPlatform by rememberSaveable { mutableStateOf(coreSystems.rmmPlatform) }
    var lineOfBusinessApps by rememberSaveable { mutableStateOf(coreSystems.lineOfBusinessApps) }
    var notes by rememberSaveable { mutableStateOf(coreSystems.notes) }

    FormDialog("Core Systems", onDismiss, onSave = {
        onSave(
            CoreSystems(
                emailPlatform = emailPlatform,
                emailDomain = emailDomain.trim(),
                adminPortal = adminPortal.trim(),
                mfaStatus = mfaStatus,
                fileStorage = fileStorage,
                backupPlatform = backupPlatform,
                securityPlatform = securityPlatform,
                firewallPlatform = firewallPlatform,
                wifiPlatform = wifiPlatform,
                rmmPlatform = rmmPlatform,
                lineOfBusinessApps = lineOfBusinessApps.trim(),
                notes = notes.trim()
            )
        )
    }) {
        HintText("Use this for platform inventory. Put troubleshooting details in Notes.")
        SimpleDropdown("Email Platform", emailPlatform, EMAIL_PLATFORMS) { emailPlatform = it }
        FormTextField("Email Domain", emailDomain) { emailDomain = it }
        FormTextField("Admin Portal / Tenant", adminPortal) { adminPortal = it }
        SimpleDropdown("MFA Status", mfaStatus, MFA_STATUSES) { mfaStatus = it }
        SimpleDropdown("File Storage", fileStorage, FILE_STORAGE_PLATFORMS) { fileStorage = it }
        SimpleDropdown("Backup Platform", backupPlatform, BACKUP_PLATFORMS) { backupPlatform = it }
        SimpleDropdown("Security / EDR", securityPlatform, SECURITY_PLATFORMS) { securityPlatform = it }
        SimpleDropdown("Firewall", firewallPlatform, FIREWALL_PLATFORMS) { firewallPlatform = it }
        SimpleDropdown("Wi-Fi", wifiPlatform, WIFI_PLATFORMS) { wifiPlatform = it }
        SimpleDropdown("RMM", rmmPlatform, RMM_PLATFORMS) { rmmPlatform = it }
        FormTextField("Line-of-Business Apps", lineOfBusinessApps, minLines = 2) { lineOfBusinessApps = it }
        FormTextField("Core Systems Notes", notes, minLines = 3) { notes = it }
    }
}

@Composable
fun VisitDialog(visit: SiteVisit, onDismiss: () -> Unit, onSave: (SiteVisit) -> Unit) {
    var client by rememberSaveable(visit.id) { mutableStateOf(visit.clientName) }
    var project by rememberSaveable(visit.id) { mutableStateOf(visit.projectName) }
    var tech by rememberSaveable(visit.id) { mutableStateOf(visit.technicianName) }
    var date by rememberSaveable(visit.id) { mutableStateOf(visit.date) }
    var jobType by rememberSaveable(visit.id) { mutableStateOf(visit.jobType) }
    var clientType by rememberSaveable(visit.id) { mutableStateOf(visit.clientTypeLabel) }
    FormDialog("Edit Visit", onDismiss, onSave = {
        if (client.isBlank() && project.isBlank()) return@FormDialog
        onSave(visit.copy(clientName = client.trim(), projectName = project.trim(), technicianName = tech.trim(), date = date.trim(), jobType = jobType.trim().ifBlank { "General Service Call" }, clientType = clientType.trim().ifBlank { "General Business" }))
    }) {
        FormTextField("Client / Site Name", client) { client = it }
        FormTextField("Project Name", project) { project = it }
        FormTextField("Technician Name", tech) { tech = it }
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Client Type", clientType, CLIENT_TYPES) { clientType = it }
        SimpleDropdown("Job Type", jobType, JOB_TYPES) { jobType = it }
        TemplatePreviewCard(auditTemplateFor(clientType))
    }
}

@Composable
private fun ConfirmDeleteDialog(request: DeleteRequest, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${request.kind.name.lowercase(Locale.US)}?") },
        text = { Text("This will remove \"${request.title}\" from this device. This cannot be undone.") },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun NoteDialog(existing: VisitNote? = null, onDismiss: () -> Unit, onSave: (VisitNote) -> Unit) {
    var location by rememberSaveable(existing?.id) { mutableStateOf(existing?.location.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: "General Note") }
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    FormDialog(if (existing == null) "Add Note" else "Edit Note", onDismiss, onSave = { onSave((existing ?: VisitNote()).copy(location = location.trim(), category = category.trim(), title = title.trim(), notes = notes.trim())) }) {
        FormTextField("Location", location) { location = it }
        SimpleDropdown("Category", category, NOTE_CATEGORIES) { category = it }
        FormTextField("Title", title) { title = it }
        FormTextField("Notes", notes, minLines = 4) { notes = it }
    }
}

@Composable
fun DeviceDialog(existing: DeviceInfo? = null, onDismiss: () -> Unit, onSave: (DeviceInfo) -> Unit) {
    var location by rememberSaveable(existing?.id) { mutableStateOf(existing?.location.orEmpty()) }
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.deviceType ?: "Other") }
    var manufacturer by rememberSaveable(existing?.id) { mutableStateOf(existing?.manufacturer.orEmpty()) }
    var model by rememberSaveable(existing?.id) { mutableStateOf(existing?.model.orEmpty()) }
    var serial by rememberSaveable(existing?.id) { mutableStateOf(existing?.serialNumber.orEmpty()) }
    var mac by rememberSaveable(existing?.id) { mutableStateOf(existing?.macAddress.orEmpty()) }
    var ip by rememberSaveable(existing?.id) { mutableStateOf(existing?.ipAddress.orEmpty()) }
    var hostname by rememberSaveable(existing?.id) { mutableStateOf(existing?.hostname.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    FormDialog(if (existing == null) "Add Device" else "Edit Device", onDismiss, onSave = { onSave((existing ?: DeviceInfo()).copy(location = location.trim(), deviceType = type.trim(), manufacturer = manufacturer.trim(), model = model.trim(), serialNumber = serial.trim(), macAddress = mac.trim(), ipAddress = ip.trim(), hostname = hostname.trim(), notes = notes.trim())) }) {
        FormTextField("Location", location) { location = it }
        SimpleDropdown("Device Type", type, DEVICE_TYPES) { type = it }
        FormTextField("Manufacturer", manufacturer) { manufacturer = it }
        FormTextField("Model", model) { model = it }
        FormTextField("Serial Number", serial) { serial = it }
        FormTextField("MAC Address", mac) { mac = it }
        FormTextField("IP Address", ip) { ip = it }
        FormTextField("Hostname", hostname) { hostname = it }
        FormTextField("Notes", notes, minLines = 3) { notes = it }
    }
}

@Composable
fun PdfExportOptionsDialog(visit: SiteVisit, isExporting: Boolean = false, onDismiss: () -> Unit, onExport: (PdfExportOptions) -> Unit) {
    var exportMode by rememberSaveable { mutableStateOf("Email-Friendly PDF") }
    var photoQuality by rememberSaveable { mutableStateOf("Medium") }
    var photosToInclude by rememberSaveable { mutableStateOf(if (visit.photos.any { it.isKeyPhoto }) "Key Photos Only" else "All Photos") }
    var includeNotes by rememberSaveable { mutableStateOf(true) }
    var includeDevices by rememberSaveable { mutableStateOf(true) }
    var includeExpenses by rememberSaveable { mutableStateOf(true) }
    var includeExpenseReceipts by rememberSaveable { mutableStateOf(true) }
    var includePhotos by rememberSaveable { mutableStateOf(true) }
    val keyPhotoCount = visit.photos.count { it.isKeyPhoto }
    val selectedPhotoCount = if (photosToInclude == "Key Photos Only") keyPhotoCount else visit.photos.size
    val atLeastOneDetail = includeNotes || includeDevices || includeExpenses || includePhotos
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export / Share Report") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Control report size so big photo-heavy visits can still be emailed.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                if (isExporting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Creating PDF in the background. Large photo reports may take a minute.", color = GalvyxCyan, fontSize = 12.sp)
                    }
                }
                if (visit.photos.size >= 12) HintText("Size warning: this visit has ${visit.photos.size} photos. Email-friendly mode with key photos is recommended.")
                SimpleDropdown("Export Mode", exportMode, listOf("Email-Friendly PDF", "Full Evidence PDF")) { mode ->
                    exportMode = mode
                    if (mode == "Email-Friendly PDF") {
                        photoQuality = "Medium"
                        if (keyPhotoCount > 0) photosToInclude = "Key Photos Only"
                    } else {
                        photoQuality = "Original / Archive"
                        photosToInclude = "All Photos"
                    }
                }
                SimpleDropdown("Photo Quality", photoQuality, listOf("Small", "Medium", "Original / Archive")) { photoQuality = it }
                SimpleDropdown("Photos", photosToInclude, listOf("Key Photos Only", "All Photos")) { photosToInclude = it }
                Text("Selected for PDF: $selectedPhotoCount of ${visit.photos.size} photo(s). Key photos: $keyPhotoCount.", color = GalvyxCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                ExportOptionRow("Notes", "${visit.notes.size} note(s)", includeNotes) { includeNotes = it }
                ExportOptionRow("Devices", "${visit.devices.size} device(s)", includeDevices) { includeDevices = it }
                ExportOptionRow("Expenses", "${visit.expenses.size} expense(s)", includeExpenses) { checked ->
                    includeExpenses = checked
                    if (!checked) includeExpenseReceipts = false
                }
                ExportOptionRow("Expense receipt images", "Receipt photos under matching expenses", includeExpenseReceipts, enabled = includeExpenses) { includeExpenseReceipts = it }
                ExportOptionRow("General photos", "${visit.photos.size} photo(s)", includePhotos) { includePhotos = it }
                if (photosToInclude == "Key Photos Only" && keyPhotoCount == 0) HintText("No key photos are marked yet, so Galvyx will include all photos to avoid exporting an empty photo section.")
                if (!atLeastOneDetail) HintText("Visit Summary is always included. Select at least one detail section for a useful report.")
            }
        },
        confirmButton = {
            Button(enabled = !isExporting, onClick = {
                onExport(
                    PdfExportOptions(
                        includeNotes = includeNotes,
                        includeDevices = includeDevices,
                        includeExpenses = includeExpenses,
                        includeExpenseReceipts = includeExpenses && includeExpenseReceipts,
                        includePhotos = includePhotos,
                        exportMode = exportMode,
                        photoQuality = photoQuality,
                        photosToInclude = photosToInclude
                    )
                )
            }) { Text(if (isExporting) "Creating..." else "Create PDF") }
        },
        dismissButton = { TextButton(enabled = !isExporting, onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExportOptionRow(label: String, detail: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExpenseDialog(
    existing: VisitExpense? = null,
    onDismiss: () -> Unit,
    onSave: (VisitExpense) -> Unit,
    onSaveAndScanReceipt: ((VisitExpense) -> Unit)? = null,
    onDeleteReceipt: ((String, String) -> Unit)? = null
) {
    var date by rememberSaveable(existing?.id) { mutableStateOf(existing?.date ?: todayString()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: "Other") }
    var vendor by rememberSaveable(existing?.id) { mutableStateOf(existing?.vendor.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableStateOf(existing?.amount.orEmpty()) }
    var payment by rememberSaveable(existing?.id) { mutableStateOf(existing?.paymentMethod ?: "Reimbursable") }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var receiptPaths by rememberSaveable(existing?.id) { mutableStateOf(existing?.receiptPhotoPaths ?: emptyList()) }
    fun currentExpense(): VisitExpense = (existing ?: VisitExpense()).copy(
        date = date.trim(),
        category = category.trim(),
        vendor = vendor.trim(),
        amount = amount.trim(),
        paymentMethod = payment.trim(),
        notes = notes.trim(),
        receiptPhotoPaths = receiptPaths
    )
    FormDialog(if (existing == null) "Add Expense" else "Edit Expense", onDismiss, onSave = { onSave(currentExpense()) }) {
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Category", category, EXPENSE_CATEGORIES) { category = it }
        FormTextField("Vendor / Merchant", vendor) { vendor = it }
        FormTextField("Amount", amount, keyboardType = KeyboardType.Decimal) { amount = it }
        SimpleDropdown("Payment Method", payment, PAYMENT_METHODS) { payment = it }
        FormTextField("Notes", notes, minLines = 3) { notes = it }
        if (existing != null) {
            ReceiptManagerSection(
                receiptPaths = receiptPaths,
                onDeleteReceipt = { path ->
                    receiptPaths = receiptPaths.filterNot { it == path }
                    onDeleteReceipt?.invoke(existing.id, path)
                }
            )
        }
        if (onSaveAndScanReceipt != null) {
            OutlinedButton(onClick = { onSaveAndScanReceipt(currentExpense()) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (existing == null) "Save & Document Scan Receipt" else "Save & Scan Another Receipt")
            }
            HintText("Use this from the expense form when you are entering mileage, meal, parts, or other advanced expense details.")
        }
    }
}

@Composable
fun ReceiptManagerSection(receiptPaths: List<String>, onDeleteReceipt: (String) -> Unit) {
    if (receiptPaths.isEmpty()) {
        HintText("No receipts attached yet. Save & scan to add one.")
        return
    }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Attached receipts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GalvyxCyan)
        receiptPaths.forEachIndexed { index, path ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = GalvyxCardElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val bitmap = remember(path) { loadBitmap(context, path) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receipt scan ${index + 1}",
                            modifier = Modifier.size(width = 72.dp, height = 54.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(width = 72.dp, height = 54.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Box(contentAlignment = Alignment.Center) { Text("Receipt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receipt scan ${index + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Attached to this expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onDeleteReceipt(path) }) { Text("Remove") }
                }
            }
        }
        HintText("Removing a receipt only removes it from this expense/report; app-owned scan files are cleaned up safely.")
    }
}

@Composable
fun PhotoCaptionDialog(existing: VisitPhoto? = null, onDismiss: () -> Unit, onSave: (VisitPhoto) -> Unit) {
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: "General") }
    var stage by rememberSaveable(existing?.id) { mutableStateOf(existing?.stage ?: "Reference") }
    var caption by rememberSaveable(existing?.id) { mutableStateOf(existing?.caption.orEmpty()) }
    var rotationDegrees by rememberSaveable(existing?.id) { mutableStateOf(existing?.rotationDegrees ?: 0) }
    var isKeyPhoto by rememberSaveable(existing?.id) { mutableStateOf(existing?.isKeyPhoto ?: false) }
    FormDialog(if (existing == null) "Photo Saved" else "Edit Photo Details", onDismiss, saveLabel = if (existing == null) "Attach Photo" else "Save Photo", onSave = {
        onSave(
            (existing ?: VisitPhoto(path = "")).copy(
                category = category.trim().ifBlank { "General" },
                stage = stage.trim().ifBlank { "Reference" },
                caption = caption.trim(),
                rotationDegrees = rotationDegrees.normalizedRotationDegrees(),
                isKeyPhoto = isKeyPhoto
            )
        )
    }) {
        Text("Tag the photo so reports can group before/after work and keep email PDFs smaller.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        SimpleDropdown("Category", category, PHOTO_CATEGORIES) { category = it }
        SimpleDropdown("Stage", stage, PHOTO_STAGES) { stage = it }
        FormTextField("Caption", caption) { caption = it }
        ExportOptionRow("Key photo", "Include this in email-friendly reports", isKeyPhoto) { isKeyPhoto = it }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { rotationDegrees = (rotationDegrees + 270).normalizedRotationDegrees() }, modifier = Modifier.weight(1f)) { Text("Rotate Left") }
            Text("${rotationDegrees.normalizedRotationDegrees()}°", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { rotationDegrees = (rotationDegrees + 90).normalizedRotationDegrees() }, modifier = Modifier.weight(1f)) { Text("Rotate Right") }
        }
        HintText("Galvyx auto-fixes camera orientation from the photo metadata. Use rotate only if a picture still looks sideways.")
    }
}

@Composable
fun FormDialog(title: String, onDismiss: () -> Unit, saveLabel: String = "Save", onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { Button(onClick = onSave) { Text(saveLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CardPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.82f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun CapabilityStrip() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniCapability("photo", "Photos", GalvyxCyan, Modifier.weight(1f))
        MiniCapability("devices", "Devices", GalvyxVioletBright, Modifier.weight(1f))
        MiniCapability("pdf", "PDFs", GalvyxAlienGreen, Modifier.weight(1f))
    }
}

@Composable
fun MiniCapability(kind: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CapabilityGlyph(kind = kind, accent = accent)
            Text(label, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CapabilityGlyph(kind: String, accent: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.1f)
        when (kind) {
            "photo" -> {
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.30f),
                    size = Size(size.width * 0.68f, size.height * 0.48f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.75f),
                    topLeft = Offset(size.width * 0.28f, size.height * 0.20f),
                    size = Size(size.width * 0.22f, size.height * 0.14f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawCircle(color = accent.copy(alpha = 0.9f), radius = size.minDimension * 0.13f, center = center, style = stroke)
                drawCircle(color = accent.copy(alpha = 0.32f), radius = size.minDimension * 0.05f, center = center)
            }
            "devices" -> {
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    topLeft = Offset(size.width * 0.20f, size.height * 0.18f),
                    size = Size(size.width * 0.60f, size.height * 0.40f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(accent.copy(alpha = 0.78f), Offset(size.width * 0.50f, size.height * 0.58f), Offset(size.width * 0.50f, size.height * 0.78f), strokeWidth = 2.1f)
                drawLine(accent.copy(alpha = 0.78f), Offset(size.width * 0.30f, size.height * 0.78f), Offset(size.width * 0.70f, size.height * 0.78f), strokeWidth = 2.1f)
                listOf(0.25f to 0.78f, 0.50f to 0.82f, 0.75f to 0.78f).forEach { (x, y) ->
                    drawCircle(color = accent.copy(alpha = 0.9f), radius = size.minDimension * 0.045f, center = Offset(size.width * x, size.height * y))
                }
            }
            else -> {
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    topLeft = Offset(size.width * 0.25f, size.height * 0.12f),
                    size = Size(size.width * 0.50f, size.height * 0.72f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawLine(accent.copy(alpha = 0.75f), Offset(size.width * 0.38f, size.height * 0.36f), Offset(size.width * 0.62f, size.height * 0.36f), strokeWidth = 2f)
                drawLine(accent.copy(alpha = 0.75f), Offset(size.width * 0.38f, size.height * 0.50f), Offset(size.width * 0.62f, size.height * 0.50f), strokeWidth = 2f)
                drawLine(accent.copy(alpha = 0.75f), Offset(size.width * 0.38f, size.height * 0.64f), Offset(size.width * 0.56f, size.height * 0.64f), strokeWidth = 2f)
            }
        }
    }
}

@Composable
fun NeonPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    ) {
        Text(
            text = text.ifBlank { "General" },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
fun VisitMetricRow(
    notes: Int,
    photos: Int,
    devices: Int,
    expenses: Int,
    expenseTotal: Double? = null,
    compact: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("Notes", notes.toString(), GalvyxCyan, Modifier.weight(1f), compact)
            MetricTile("Photos", photos.toString(), GalvyxVioletBright, Modifier.weight(1f), compact)
            MetricTile("Devices", devices.toString(), GalvyxAlienGreen, Modifier.weight(1f), compact)
            MetricTile("Expenses", expenses.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f), compact)
        }
        if (expenseTotal != null && expenseTotal > 0.0) {
            MetricTile("Expense Total", expenseTotal.toCurrencyString(), MaterialTheme.colorScheme.secondary, Modifier.fillMaxWidth(), compact = false)
        }
    }
}

@Composable
fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = GalvyxCardElevated.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 8.dp else 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = if (compact) 15.sp else 18.sp, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun FormColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun FormTextField(label: String, value: String, minLines: Int = 1, keyboardType: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SimpleDropdown(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$label: $selected", textAlign = TextAlign.Start)
                Text("⌄", color = GalvyxCyan, fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FolderPickerRow(
    title: String,
    uri: String,
    emptyText: String = "Using Galvyx app storage",
    clearText: String = "Use Default",
    onChoose: () -> Unit,
    onClear: () -> Unit
) {
    CardPanel {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(if (uri.isBlank()) emptyText else folderLabel(uri), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onChoose, modifier = Modifier.weight(1f)) { Text(if (uri.isBlank()) "Choose Folder" else "Change Folder") }
            if (uri.isNotBlank()) OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(clearText) }
        }
    }
}

@Composable
fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
fun HomeSecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun TwoColumnActions(vararg actions: Pair<String, () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        actions.toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { action -> OutlinedButton(onClick = action.second, modifier = Modifier.weight(1f)) { Text(action.first) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(count.toString(), color = GalvyxCyan, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailCard(title: String, subtitle: String, body: String, onEdit: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title.ifBlank { "Untitled" }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onEdit != null) TextButton(onClick = onEdit) { Text("Edit") }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
        if (body.isNotBlank()) Text(body, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ExpenseBreakdownCard(breakdown: ExpenseBreakdown) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Updates as receipts and expense amounts are added.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            NeonPill(breakdown.total.toCurrencyString(), GalvyxAlienGreen)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreakdownMiniTile("Reimbursable", breakdown.reimbursableTotal.toCurrencyString(), GalvyxCyan, Modifier.weight(1f))
            BreakdownMiniTile("Company Paid", breakdown.companyPaidTotal.toCurrencyString(), GalvyxVioletBright, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreakdownMiniTile("With Receipts", breakdown.receiptAttachedTotal.toCurrencyString(), GalvyxAlienGreen, Modifier.weight(1f))
            BreakdownMiniTile("Missing Receipts", breakdown.missingReceiptTotal.toCurrencyString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
        }
        Text("${breakdown.receiptScanCount} receipt scan(s) attached • ${breakdown.missingReceiptCount} expense(s) missing receipts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        if (breakdown.byCategory.isNotEmpty()) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            Text("By Category", fontWeight = FontWeight.SemiBold)
            breakdown.byCategory.forEach { total -> BreakdownLine(total) }
        }
        if (breakdown.byPaymentMethod.isNotEmpty()) {
            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
            Text("By Payment", fontWeight = FontWeight.SemiBold)
            breakdown.byPaymentMethod.forEach { total -> BreakdownLine(total) }
        }
    }
}

@Composable
fun BreakdownMiniTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.10f), border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun BreakdownLine(total: ExpenseGroupTotal) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${total.label} (${total.count})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(total.total.toCurrencyString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun ExpenseDetailCard(expense: VisitExpense, onScanReceipt: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${expense.vendor} ${expense.amount}".trim().ifBlank { expense.category }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${expense.date} • ${expense.category} • ${expense.paymentMethod}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(expense.receiptCountLabel, color = GalvyxCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
        if (expense.notes.isNotBlank()) Text(expense.notes, color = MaterialTheme.colorScheme.onSurface)
        OutlinedButton(onClick = onScanReceipt, modifier = Modifier.fillMaxWidth()) { Text("Document Scan Receipt")}
        if (expense.receiptPhotoPaths.isNotEmpty()) {
            val context = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                expense.receiptPhotoPaths.take(3).forEachIndexed { index, path ->
                    val bitmap = remember(path) { loadBitmap(context, path) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receipt scan ${index + 1}",
                            modifier = Modifier.weight(1f).height(92.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.weight(1f).height(92.dp), shape = RoundedCornerShape(12.dp), color = GalvyxCardElevated) {
                            Box(contentAlignment = Alignment.Center) { Text("Receipt ${index + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                if (expense.receiptPhotoPaths.size < 3) repeat(3 - expense.receiptPhotoPaths.size) { Spacer(Modifier.weight(1f)) }
            }
            if (expense.receiptPhotoPaths.size > 3) HintText("+${expense.receiptPhotoPaths.size - 3} more receipt scan(s) saved")
        } else {
            HintText("Tip: use Scan / Add Receipt Photo after entering the expense. Hold the receipt flat with good light for a clean scan-style image.")
        }
    }
}

@Composable
fun PhotoDetailCard(photo: VisitPhoto, onEdit: () -> Unit, onDelete: () -> Unit) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(photo.caption.ifBlank { photo.category.ifBlank { "Photo" } }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${photo.reportLabel}${if (photo.isKeyPhoto) " • Key Photo" else ""} • Saved locally", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
        val context = LocalContext.current
        val bitmap = remember(photo.path, photo.rotationDegrees) { loadBitmap(context, photo.path, photo.rotationDegrees) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = photo.caption.ifBlank { "Site visit photo" },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(photo.path, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SmallEmpty(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = GalvyxCardElevated) {
        Text(text, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun HintText(text: String) {
    Text(text, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun String.toMoneyOrZero(): Double = replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0

fun Double.toCurrencyString(): String = "$${String.format(Locale.US, "%.2f", this)}"

private fun defaultBackupFileName(): String = "galvyx-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.json"

private fun CompanyProfile.withClearedFolderPermissions(): CompanyProfile = copy(
    localReportsTreeUri = "",
    localPhotosTreeUri = "",
    backupTreeUri = "",
    autoBackupEnabled = false
)

private fun exportBackupJson(context: Context, uri: Uri, profile: CompanyProfile, visits: List<SiteVisit>): Boolean = runCatching {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(backupToJson(profile, visits).toByteArray(Charsets.UTF_8))
    } ?: return@runCatching false
    true
}.getOrDefault(false)

private fun saveAutoBackup(context: Context, profile: CompanyProfile, visits: List<SiteVisit>): Boolean = runCatching {
    if (!profile.autoBackupEnabled || profile.backupTreeUri.isBlank()) return@runCatching false
    val tree = DocumentFile.fromTreeUri(context, Uri.parse(profile.backupTreeUri)) ?: return@runCatching false
    tree.findFile(AUTO_BACKUP_FILE_NAME)?.delete()
    val file = tree.createFile("application/json", AUTO_BACKUP_FILE_NAME) ?: return@runCatching false
    exportBackupJson(context, file.uri, profile, visits)
}.getOrDefault(false)

private fun importBackupJson(context: Context, uri: Uri): GalvyxBackup? = runCatching {
    val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        ?: return@runCatching null
    backupFromJson(raw)
}.getOrNull()

private fun appStorageDir(context: Context, type: String, rootFolder: String, childFolder: String): File {
    val root = safeFolderSegment(rootFolder).ifBlank { "Galvyx" }
    val child = safeFolderSegment(childFolder).ifBlank { if (type == Environment.DIRECTORY_PICTURES) "Photos" else "Reports" }
    val base = context.getExternalFilesDir(type) ?: context.filesDir
    return File(base, "$root/$child")
}

private fun safeFolderSegment(value: String): String = value
    .trim()
    .replace(Regex("[^A-Za-z0-9 _.-]+"), "_")
    .replace(Regex("_+"), "_")
    .trim('_', ' ', '.', '-')

private fun deleteAppOwnedPhoto(context: Context, photoPath: String, profile: CompanyProfile): Boolean = runCatching {
    if (photoPath.startsWith("content://")) {
        return@runCatching DocumentFile.fromSingleUri(context, Uri.parse(photoPath))?.delete() ?: false
    }
    val target = File(photoPath).canonicalFile
    val currentPhotosRoot = appStorageDir(context, Environment.DIRECTORY_PICTURES, profile.localRootFolder, profile.localPhotosFolder).canonicalFile
    val legacyPhotosRoot = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Galvyx").canonicalFile
    val isAppOwned = target.path.startsWith(currentPhotosRoot.path) || target.path.startsWith(legacyPhotosRoot.path)
    isAppOwned && target.delete()
}.getOrDefault(false)

private fun createPhotoUri(context: Context, profile: CompanyProfile, prefix: String = "galvyx"): Pair<String, Uri>? = runCatching {
    val safePrefix = safeFolderSegment(prefix).ifBlank { "galvyx" }.lowercase(Locale.US)
    val fileName = "${safePrefix}_${System.currentTimeMillis()}.jpg"
    if (profile.localPhotosTreeUri.isNotBlank()) {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(profile.localPhotosTreeUri)) ?: return@runCatching null
        val file = tree.createFile("image/jpeg", fileName) ?: return@runCatching null
        return@runCatching file.uri.toString() to file.uri
    }
    val dir = appStorageDir(context, Environment.DIRECTORY_PICTURES, profile.localRootFolder, profile.localPhotosFolder)
    dir.mkdirs()
    val file = File(dir, fileName)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    file.absolutePath to uri
}.getOrNull()

private fun saveReceiptScanFromUri(context: Context, profile: CompanyProfile, sourceUri: Uri): String? = runCatching {
    val fileName = "receipt_scan_${System.currentTimeMillis()}.jpg"
    val input = context.contentResolver.openInputStream(sourceUri) ?: return@runCatching null
    input.use { source ->
        if (profile.localPhotosTreeUri.isNotBlank()) {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(profile.localPhotosTreeUri)) ?: return@runCatching null
            val file = tree.createFile("image/jpeg", fileName) ?: return@runCatching null
            val output = context.contentResolver.openOutputStream(file.uri) ?: return@runCatching null
            output.use { source.copyTo(it) }
            file.uri.toString()
        } else {
            val dir = appStorageDir(context, Environment.DIRECTORY_PICTURES, profile.localRootFolder, profile.localPhotosFolder)
            dir.mkdirs()
            val file = File(dir, fileName)
            file.outputStream().use { source.copyTo(it) }
            file.absolutePath
        }
    }
}.getOrNull()

private fun exportVisitPdf(context: Context, visit: SiteVisit, profile: CompanyProfile, options: PdfExportOptions = PdfExportOptions()): ExportedReport? = runCatching {
    val safeName = "Galvyx_${visit.clientName}_${visit.projectName}_${visit.date}_${System.currentTimeMillis()}".replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "Galvyx_Report" }
    val target = createReportTarget(context, profile, "$safeName.pdf") ?: return@runCatching null

    val document = PdfDocument()
    val pdfScale = 3f
    val logicalPageWidth = 612
    val logicalPageHeight = 792
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; isFakeBoldText = true }
    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; isFakeBoldText = true }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder((logicalPageWidth * pdfScale).toInt(), (logicalPageHeight * pdfScale).toInt(), pageNumber).create())
    var canvas = page.canvas.apply { scale(pdfScale, pdfScale) }
    var y = 48f

    fun footer() {
        canvas.drawText("Galvyx • ${visit.title.take(52)}", 42f, 770f, footerPaint)
        canvas.drawText("Page $pageNumber", 540f, 770f, footerPaint)
    }

    fun newPage() {
        footer()
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder((logicalPageWidth * pdfScale).toInt(), (logicalPageHeight * pdfScale).toInt(), pageNumber).create())
        canvas = page.canvas.apply { scale(pdfScale, pdfScale) }
        y = 48f
    }

    fun line(text: String, paint: Paint = bodyPaint, gap: Float = 17f) {
        if (y > 724f) newPage()
        canvas.drawText(text.take(95), 42f, y, paint)
        y += gap
    }

    fun wrapped(text: String) {
        val words = text.replace('\n', ' ').split(' ')
        var current = ""
        for (word in words) {
            val next = if (current.isBlank()) word else "$current $word"
            if (next.length > 88) {
                line(current)
                current = word
            } else current = next
        }
        if (current.isNotBlank()) line(current)
    }

    fun section(title: String) {
        y += 8f
        line(title, headerPaint, 22f)
    }

    line("Galvyx Site Visit Report", titlePaint, 28f)
    line("${profile.companyName.ifBlank { "Galvyx" }} • ${profile.reportFooter}")
    line("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}")
    section("Visit Summary")
    line("Client/Site: ${visit.clientName}")
    line("Project: ${visit.projectName}")
    line("Technician: ${visit.technicianName}")
    line("Date: ${visit.date}")
    line("Client Type: ${visit.clientTypeLabel}")
    line("Job Type: ${visit.jobType}")
    line("Export Mode: ${options.exportMode} • Photo Quality: ${options.photoQuality} • Photos: ${options.photosToInclude}")
    line(visit.summary())
    val auditTemplate = auditTemplateFor(visit.clientTypeLabel)
    section("MSP Client Template")
    line(auditTemplate.summary)
    line("Core Systems: ${auditTemplate.coreSystems.joinToString(", ").take(90)}")
    auditTemplate.keyQuestions.take(5).forEachIndexed { index, question -> line("Q${index + 1}: $question") }

    section("Core Systems Summary")
    val coreRows = visit.coreSystems.summaryRows
    if (coreRows.isEmpty()) line("No core systems captured.")
    coreRows.forEach { (label, value) -> wrapped("$label: $value") }

    if (options.includeNotes) {
        section("Notes")
        if (visit.notes.isEmpty()) line("No notes captured.")
        visit.notes.forEachIndexed { index, note ->
            line("${index + 1}. ${note.title.ifBlank { note.category }}", bodyPaint.apply { isFakeBoldText = true })
            bodyPaint.isFakeBoldText = false
            line("Location: ${note.location} • Category: ${note.category}")
            wrapped(note.notes)
            y += 5f
        }
    }

    if (options.includeDevices) {
        section("Devices")
        if (visit.devices.isEmpty()) line("No devices captured.")
        visit.devices.forEachIndexed { index, device ->
            line("${index + 1}. ${device.deviceType} ${device.hostname}".trim(), bodyPaint.apply { isFakeBoldText = true })
            bodyPaint.isFakeBoldText = false
            listOf(
                "Location: ${device.location}",
                "Manufacturer/Model: ${device.manufacturer} ${device.model}",
                "Serial: ${device.serialNumber}",
                "MAC: ${device.macAddress}",
                "IP: ${device.ipAddress}",
                "Notes: ${device.notes}"
            ).forEach { if (!it.endsWith(": ")) line(it) }
            y += 5f
        }
    }

    if (options.includeExpenses) {
        section("Expenses")
        if (visit.expenses.isEmpty()) line("No expenses captured.")
        if (visit.expenses.isNotEmpty()) {
            val breakdown = visit.expenseBreakdown
            line("Expense Total: ${breakdown.total.toCurrencyString()}", headerPaint, 22f)
            line("Reimbursable: ${breakdown.reimbursableTotal.toCurrencyString()} • Company Paid: ${breakdown.companyPaidTotal.toCurrencyString()}")
            line("With Receipts: ${breakdown.receiptAttachedTotal.toCurrencyString()} • Missing Receipts: ${breakdown.missingReceiptTotal.toCurrencyString()}")
            line("Receipt Scans: ${breakdown.receiptScanCount} • Expenses Missing Receipts: ${breakdown.missingReceiptCount}")
            line("By Category", headerPaint, 20f)
            breakdown.byCategory.forEach { line("${it.label}: ${it.total.toCurrencyString()} (${it.count})") }
            line("By Payment", headerPaint, 20f)
            breakdown.byPaymentMethod.forEach { line("${it.label}: ${it.total.toCurrencyString()} (${it.count})") }
            y += 4f
        }
        visit.expenses.forEachIndexed { index, expense ->
            line("${index + 1}. ${expense.vendor} ${expense.amount}".trim(), bodyPaint.apply { isFakeBoldText = true })
            bodyPaint.isFakeBoldText = false
            line("${expense.date} • ${expense.category} • ${expense.paymentMethod} • ${expense.receiptCountLabel}")
            wrapped(expense.notes)
            if (options.includeExpenseReceipts) {
                expense.receiptPhotoPaths.forEachIndexed { receiptIndex, receiptPath ->
                    line("Receipt scan ${receiptIndex + 1} - full size for reimbursement review")
                    val receiptBitmap = loadBitmapForPdf(context, receiptPath, 0, options, isReceipt = true)
                    if (receiptBitmap != null) {
                        if (y > 250f) newPage()
                        val maxWidth = 528f
                        val maxHeight = 620f
                        val widthScale = maxWidth / receiptBitmap.width.toFloat()
                        val heightScale = maxHeight / receiptBitmap.height.toFloat()
                        val scale = minOf(widthScale, heightScale)
                        val width = receiptBitmap.width * scale
                        val height = receiptBitmap.height * scale
                        val left = 42f + ((maxWidth - width) / 2f)
                        canvas.drawBitmap(receiptBitmap, null, android.graphics.RectF(left, y, left + width, y + height), imagePaint)
                        y += height + 18f
                        if (y > 680f) newPage()
                    } else {
                        line(receiptPath)
                    }
                }
            }
            y += 5f
        }
    }

    if (options.includePhotos) {
        section("Photos")
        if (visit.photos.isEmpty()) line("No photos captured.")
        if (visit.photos.isNotEmpty() && options.keyPhotosOnly && visit.photos.any { it.isKeyPhoto }) line("Showing key photos only for email-friendly report size.")
        val reportPhotos = if (options.keyPhotosOnly && visit.photos.any { it.isKeyPhoto }) visit.photos.filter { it.isKeyPhoto } else visit.photos
        reportPhotos
            .groupBy { it.category.ifBlank { "General" } }
            .forEach { (category, categoryPhotos) ->
                line(category, headerPaint, 20f)
                categoryPhotos.forEachIndexed { index, photo ->
                    val title = "${index + 1}. ${photo.stage.ifBlank { "Reference" }}${if (photo.isKeyPhoto) " • Key" else ""}${if (photo.caption.isNotBlank()) ": ${photo.caption}" else ""}"
                    line(title)
                    val bitmap = loadBitmapForPdf(context, photo.path, photo.rotationDegrees, options)
                    if (bitmap != null) {
                        if (y > 250f) newPage()
                        val maxWidth = 528f
                        val maxHeight = 620f
                        val widthScale = maxWidth / bitmap.width.toFloat()
                        val heightScale = maxHeight / bitmap.height.toFloat()
                        val scale = minOf(widthScale, heightScale)
                        val width = bitmap.width * scale
                        val height = bitmap.height * scale
                        val left = 42f + ((maxWidth - width) / 2f)
                        canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, y, left + width, y + height), imagePaint)
                        y += height + 18f
                        if (y > 680f) newPage()
                    } else {
                        line(photo.path)
                    }
                }
            }
    }

    footer()
    document.finishPage(page)
    target.outputStream.use { document.writeTo(it) }
    document.close()
    ExportedReport(target.uri, target.displayName)
}.getOrNull()

private fun createReportTarget(context: Context, profile: CompanyProfile, displayName: String): ReportTarget? {
    if (profile.localReportsTreeUri.isNotBlank()) {
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(profile.localReportsTreeUri)) ?: return null
        val file = tree.createFile("application/pdf", displayName) ?: return null
        val stream = context.contentResolver.openOutputStream(file.uri) ?: return null
        return ReportTarget(file.uri, displayName, stream)
    }
    val reportsDir = appStorageDir(context, Environment.DIRECTORY_DOCUMENTS, profile.localRootFolder, profile.localReportsFolder)
    reportsDir.mkdirs()
    val file = File(reportsDir, displayName)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return ReportTarget(uri, displayName, file.outputStream())
}

private fun loadBitmapForPdf(context: Context, reference: String, manualRotationDegrees: Int = 0, options: PdfExportOptions, isReceipt: Boolean = false): Bitmap? {
    val targetMaxDimension = if (isReceipt && options.exportMode == "Full Evidence PDF") {
        maxOf(options.maxPdfImageDimension, 1800)
    } else {
        options.maxPdfImageDimension
    }
    val source = loadScaledBitmap(context, reference, targetMaxDimension, manualRotationDegrees) ?: return null
    val maxSide = maxOf(source.width, source.height)
    val scaled = if (maxSide > targetMaxDimension) {
        val scale = targetMaxDimension.toFloat() / maxSide.toFloat()
        Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
    } else source
    return if (options.jpegQuality >= 92) {
        scaled
    } else {
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, options.jpegQuality, output)
        BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size()) ?: scaled
    }
}

private fun loadScaledBitmap(context: Context, reference: String, targetMaxDimension: Int, manualRotationDegrees: Int = 0): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    if (reference.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(reference))?.use { BitmapFactory.decodeStream(it, null, bounds) }
    } else {
        BitmapFactory.decodeFile(reference, bounds)
    }
    val sourceMaxSide = maxOf(bounds.outWidth, bounds.outHeight)
    if (sourceMaxSide <= 0) return@runCatching loadBitmap(context, reference, manualRotationDegrees)

    var sampleSize = 1
    while ((sourceMaxSide / (sampleSize * 2)) >= targetMaxDimension) {
        sampleSize *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = if (reference.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(reference))?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    } else {
        BitmapFactory.decodeFile(reference, decodeOptions)
    } ?: return@runCatching null
    val autoRotation = readExifRotationDegrees(context, reference)
    decoded.rotateBitmap((autoRotation + manualRotationDegrees).normalizedRotationDegrees())
}.getOrNull()

private fun loadBitmap(context: Context, reference: String, manualRotationDegrees: Int = 0): Bitmap? = runCatching {
    val bitmap = if (reference.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(reference))?.use { BitmapFactory.decodeStream(it) }
    } else {
        BitmapFactory.decodeFile(reference)
    } ?: return@runCatching null
    val autoRotation = readExifRotationDegrees(context, reference)
    bitmap.rotateBitmap((autoRotation + manualRotationDegrees).normalizedRotationDegrees())
}.getOrNull()

private fun readExifRotationDegrees(context: Context, reference: String): Int = runCatching {
    val orientation = if (reference.startsWith("content://")) {
        context.contentResolver.openInputStream(Uri.parse(reference))?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    } else {
        ExifInterface(reference).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}.getOrDefault(0)

private fun Bitmap.rotateBitmap(degrees: Int): Bitmap {
    if (degrees.normalizedRotationDegrees() == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun folderLabel(uri: String): String = Uri.parse(uri).lastPathSegment
    ?.substringAfterLast(':')
    ?.ifBlank { null }
    ?: "Selected folder"

private fun shareReport(context: Context, report: ExportedReport) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, report.uri)
        putExtra(Intent.EXTRA_SUBJECT, report.displayName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Galvyx report"))
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GalvyxTheme { HomeScreen(visitCount = 3) }
}
