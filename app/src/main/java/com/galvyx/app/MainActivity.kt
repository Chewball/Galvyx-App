package com.galvyx.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.galvyx.app.ui.theme.GalvyxCardElevated
import com.galvyx.app.ui.theme.GalvyxCyan
import com.galvyx.app.ui.theme.GalvyxTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "galvyx_local_store"
private const val PREF_VISITS = "visits"
private const val PREF_PROFILE = "profile"

private val JOB_TYPES = listOf("General Service Call", "Network Survey", "Camera / Security", "Access Point / Wi-Fi", "Workstation Replacement", "Server / Firewall", "Low Voltage / Cabling", "Inspection", "Other")
private val NOTE_CATEGORIES = listOf("General Note", "Network Port", "Switch / Firewall", "Access Point / Wi-Fi", "Camera", "Workstation", "Server", "Cable / Low Voltage", "Power", "Expense / Receipt", "Issue Found", "Completed Work", "Follow-Up Needed")
private val DEVICE_TYPES = listOf("Firewall", "Switch", "Access Point", "Camera", "NVR / DVR", "Server", "Workstation", "Printer", "Payment Terminal", "Time Clock", "UPS / Battery Backup", "Other")
private val EXPENSE_CATEGORIES = listOf("Meal", "Gas", "Hotel", "Parking", "Tools / Supplies", "Equipment", "Mileage", "Toll", "Shipping", "Other")
private val PAYMENT_METHODS = listOf("Personal Card", "Company Card", "Cash", "Reimbursable", "Other")

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
private enum class DialogKind { None, Note, Device, Expense, PhotoCaption, EditVisit, EditNote, EditDevice, EditExpense, EditPhoto }
private enum class DeleteKind { Visit, Note, Device, Expense, Photo }
private data class DeleteRequest(val kind: DeleteKind, val id: String, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalvyxApp(context: Context) {
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
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun persistVisits() {
        prefs.edit().putString(PREF_VISITS, visitsToJson(visits)).apply()
    }

    fun persistProfile(next: CompanyProfile) {
        profile = next
        prefs.edit().putString(PREF_PROFILE, next.toJson().toString()).apply()
    }

    fun upsertVisit(visit: SiteVisit) {
        val index = visits.indexOfFirst { it.id == visit.id }
        if (index >= 0) visits[index] = visit else visits.add(0, visit)
        persistVisits()
    }

    fun selectedVisit(): SiteVisit? = visits.firstOrNull { it.id == selectedVisitId }

    fun openVisit(visit: SiteVisit) {
        selectedVisitId = visit.id
        screen = Screen.VisitDetail
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingPhotoPath != null) {
            dialog = DialogKind.PhotoCaption
        } else {
            pendingPhotoPath = null
            pendingPhotoUri = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (screen != Screen.Home) {
                TopAppBar(
                    title = { Text(screen.title()) },
                    navigationIcon = {
                        TextButton(onClick = { screen = if (screen == Screen.VisitDetail) Screen.Recent else Screen.Home }) {
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
                        onAddPhoto = {
                            val created = createPhotoUri(context)
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
                        onEditPhoto = { photo -> editingItemId = photo.id; dialog = DialogKind.EditPhoto },
                        onDeleteNote = { note -> deleteRequest = DeleteRequest(DeleteKind.Note, note.id, note.title.ifBlank { note.category }) },
                        onDeleteDevice = { device -> deleteRequest = DeleteRequest(DeleteKind.Device, device.id, device.hostname.ifBlank { device.deviceType }) },
                        onDeleteExpense = { expense -> deleteRequest = DeleteRequest(DeleteKind.Expense, expense.id, expense.vendor.ifBlank { expense.category }) },
                        onDeletePhoto = { photo -> deleteRequest = DeleteRequest(DeleteKind.Photo, photo.id, photo.caption.ifBlank { "Photo" }) },
                        onExport = {
                            val pdfFile = exportVisitPdf(context, visit, profile)
                            if (pdfFile != null) sharePdf(context, pdfFile) else Toast.makeText(context, "PDF export failed", Toast.LENGTH_LONG).show()
                        }
                    )
                } ?: EmptyState("Visit not found", "Go back and choose a recent site visit.")
                Screen.Settings -> SettingsScreen(profile = profile, onSave = ::persistProfile)
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
                onSave = { expense -> upsertVisit(visit.copy(expenses = visit.expenses + expense)); dialog = DialogKind.None }
            )
            DialogKind.EditExpense -> visit.expenses.firstOrNull { it.id == editingItemId }?.let { existing ->
                ExpenseDialog(
                    existing = existing,
                    onDismiss = { editingItemId = null; dialog = DialogKind.None },
                    onSave = { expense -> upsertVisit(visit.copy(expenses = visit.expenses.map { if (it.id == expense.id) expense else it })); editingItemId = null; dialog = DialogKind.None }
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
                    DeleteKind.Expense -> selectedVisit()?.let { current -> upsertVisit(current.copy(expenses = current.expenses.filterNot { it.id == request.id })) }
                    DeleteKind.Photo -> selectedVisit()?.let { current ->
                        current.photos.firstOrNull { it.id == request.id }?.let { deleteAppOwnedPhoto(context, it.path) }
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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .size(88.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.95f),
                        secondary.copy(alpha = 0.38f),
                        surface.copy(alpha = 0.18f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(88.dp)) {
            drawCircle(
                color = secondary.copy(alpha = 0.18f),
                radius = size.minDimension / 2.3f,
                center = center
            )
            drawArc(
                color = secondary.copy(alpha = 0.9f),
                startAngle = -18f,
                sweepAngle = 230f,
                useCenter = false,
                topLeft = Offset(size.width * 0.12f, size.height * 0.30f),
                size = Size(size.width * 0.76f, size.height * 0.40f),
                style = Stroke(width = 3.5f)
            )
            drawArc(
                color = primary.copy(alpha = 0.75f),
                startAngle = 205f,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(size.width * 0.18f, size.height * 0.24f),
                size = Size(size.width * 0.64f, size.height * 0.52f),
                style = Stroke(width = 2.2f)
            )
            drawCircle(
                color = secondary,
                radius = 3.8f,
                center = Offset(size.width * 0.80f, size.height * 0.40f)
            )
        }

        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = primary.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, secondary.copy(alpha = 0.55f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "G",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = onPrimary
                )
            }
        }
    }
}


@Composable
fun NewVisitScreen(defaultTechnician: String, onSave: (SiteVisit) -> Unit) {
    var client by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var tech by rememberSaveable { mutableStateOf(defaultTechnician) }
    var date by rememberSaveable { mutableStateOf(todayString()) }
    var jobType by rememberSaveable { mutableStateOf("General Service Call") }

    FormColumn {
        Text("Create the shell first, then add notes/photos/devices/expenses from the visit page.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormTextField("Client / Site Name", client) { client = it }
        FormTextField("Project Name", project) { project = it }
        FormTextField("Technician Name", tech) { tech = it }
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Job Type", jobType, JOB_TYPES) { jobType = it }
        PrimaryAction("Save Site Visit") {
            if (client.isBlank() && project.isBlank()) return@PrimaryAction
            onSave(SiteVisit(clientName = client.trim(), projectName = project.trim(), technicianName = tech.trim(), date = date.trim(), jobType = jobType.trim().ifBlank { "General Service Call" }))
        }
    }
}

@Composable
fun RecentVisitsScreen(visits: List<SiteVisit>, onOpen: (SiteVisit) -> Unit, onDelete: (SiteVisit) -> Unit, onNewVisit: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var jobFilter by rememberSaveable { mutableStateOf("All Job Types") }
    val jobOptions = listOf("All Job Types") + JOB_TYPES
    val filteredVisits = visits.filter { visit ->
        visit.matchesSearch(query) && (jobFilter == "All Job Types" || visit.jobType == jobFilter)
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
                Text("${filteredVisits.size} of ${visits.size} visits", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        if (filteredVisits.isEmpty()) {
            item { SmallEmpty("No visits match your search or filter") }
        }
        items(filteredVisits, key = { it.id }) { visit ->
            CardPanel {
                Text(visit.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${visit.date} • ${visit.jobType}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onEditNote: (VisitNote) -> Unit,
    onEditDevice: (DeviceInfo) -> Unit,
    onEditExpense: (VisitExpense) -> Unit,
    onEditPhoto: (VisitPhoto) -> Unit,
    onDeleteNote: (VisitNote) -> Unit,
    onDeleteDevice: (DeviceInfo) -> Unit,
    onDeleteExpense: (VisitExpense) -> Unit,
    onDeletePhoto: (VisitPhoto) -> Unit,
    onExport: () -> Unit
) {
    val expenseTotal = visit.expenses.sumOf { it.amount.toMoneyOrZero() }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CardPanel {
                Text(visit.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${visit.date} • ${visit.technicianName.ifBlank { "Technician not set" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(visit.jobType, color = GalvyxCyan, fontWeight = FontWeight.SemiBold)
                Text(visit.summary(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (visit.expenses.isNotEmpty()) Text("Expense total: ${expenseTotal.toCurrencyString()}", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.25f))
                TwoColumnActions(
                    "Add Note" to onAddNote,
                    "Add Photo" to onAddPhoto,
                    "Add Device" to onAddDevice,
                    "Add Expense" to onAddExpense,
                    "Edit Visit" to onEditVisit
                )
                PrimaryAction("Export / Share PDF Report", onClick = onExport)
            }
        }
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
            DetailCard("${expense.vendor} ${expense.amount}".trim(), "${expense.date} • ${expense.category} • ${expense.paymentMethod}", expense.notes, onEdit = { onEditExpense(expense) }, onDelete = { onDeleteExpense(expense) })
        }
        item { SectionHeader("Photos", visit.photos.size) }
        if (visit.photos.isEmpty()) item { SmallEmpty("No photos yet") }
        items(visit.photos, key = { it.id }) { photo ->
            PhotoDetailCard(photo = photo, onEdit = { onEditPhoto(photo) }, onDelete = { onDeletePhoto(photo) })
        }
    }
}

@Composable
fun SettingsScreen(profile: CompanyProfile, onSave: (CompanyProfile) -> Unit) {
    var company by rememberSaveable(profile.companyName) { mutableStateOf(profile.companyName) }
    var tech by rememberSaveable(profile.technicianName) { mutableStateOf(profile.technicianName) }
    var footer by rememberSaveable(profile.reportFooter) { mutableStateOf(profile.reportFooter) }
    var storageMode by rememberSaveable(profile.storageMode.name) { mutableStateOf(profile.storageMode.label) }
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
                    sharePointSiteUrl = sharePointSiteUrl.trim(),
                    sharePointLibraryName = sharePointLibrary.trim().ifBlank { "Documents" },
                    sharePointFolderPath = sharePointFolder.trim().ifBlank { "Galvyx/Site Visits" }
                )
            )
        }
    }
}

@Composable
fun VisitDialog(visit: SiteVisit, onDismiss: () -> Unit, onSave: (SiteVisit) -> Unit) {
    var client by rememberSaveable(visit.id) { mutableStateOf(visit.clientName) }
    var project by rememberSaveable(visit.id) { mutableStateOf(visit.projectName) }
    var tech by rememberSaveable(visit.id) { mutableStateOf(visit.technicianName) }
    var date by rememberSaveable(visit.id) { mutableStateOf(visit.date) }
    var jobType by rememberSaveable(visit.id) { mutableStateOf(visit.jobType) }
    FormDialog("Edit Visit", onDismiss, onSave = {
        if (client.isBlank() && project.isBlank()) return@FormDialog
        onSave(visit.copy(clientName = client.trim(), projectName = project.trim(), technicianName = tech.trim(), date = date.trim(), jobType = jobType.trim().ifBlank { "General Service Call" }))
    }) {
        FormTextField("Client / Site Name", client) { client = it }
        FormTextField("Project Name", project) { project = it }
        FormTextField("Technician Name", tech) { tech = it }
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Job Type", jobType, JOB_TYPES) { jobType = it }
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
fun ExpenseDialog(existing: VisitExpense? = null, onDismiss: () -> Unit, onSave: (VisitExpense) -> Unit) {
    var date by rememberSaveable(existing?.id) { mutableStateOf(existing?.date ?: todayString()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: "Other") }
    var vendor by rememberSaveable(existing?.id) { mutableStateOf(existing?.vendor.orEmpty()) }
    var amount by rememberSaveable(existing?.id) { mutableStateOf(existing?.amount.orEmpty()) }
    var payment by rememberSaveable(existing?.id) { mutableStateOf(existing?.paymentMethod ?: "Reimbursable") }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    FormDialog(if (existing == null) "Add Expense" else "Edit Expense", onDismiss, onSave = { onSave((existing ?: VisitExpense()).copy(date = date.trim(), category = category.trim(), vendor = vendor.trim(), amount = amount.trim(), paymentMethod = payment.trim(), notes = notes.trim())) }) {
        FormTextField("Date", date) { date = it }
        SimpleDropdown("Category", category, EXPENSE_CATEGORIES) { category = it }
        FormTextField("Vendor / Merchant", vendor) { vendor = it }
        FormTextField("Amount", amount, keyboardType = KeyboardType.Decimal) { amount = it }
        SimpleDropdown("Payment Method", payment, PAYMENT_METHODS) { payment = it }
        FormTextField("Notes", notes, minLines = 3) { notes = it }
    }
}

@Composable
fun PhotoCaptionDialog(existing: VisitPhoto? = null, onDismiss: () -> Unit, onSave: (VisitPhoto) -> Unit) {
    var caption by rememberSaveable(existing?.id) { mutableStateOf(existing?.caption.orEmpty()) }
    FormDialog(if (existing == null) "Photo Saved" else "Edit Photo Caption", onDismiss, saveLabel = if (existing == null) "Attach Photo" else "Save Caption", onSave = { onSave((existing ?: VisitPhoto(path = "")).copy(caption = caption.trim())) }) {
        Text("Add a short caption so the report makes sense later.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FormTextField("Caption", caption) { caption = it }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
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
fun PhotoDetailCard(photo: VisitPhoto, onEdit: () -> Unit, onDelete: () -> Unit) {
    CardPanel {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(photo.caption.ifBlank { "Photo" }, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Saved locally", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
        val bitmap = remember(photo.path) { BitmapFactory.decodeFile(photo.path) }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = photo.caption.ifBlank { "Site visit photo" },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
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

private fun deleteAppOwnedPhoto(context: Context, photoPath: String): Boolean = runCatching {
    val photosRoot = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Galvyx").canonicalFile
    val target = File(photoPath).canonicalFile
    target.path.startsWith(photosRoot.path) && target.delete()
}.getOrDefault(false)

private fun createPhotoUri(context: Context): Pair<String, Uri>? = runCatching {
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Galvyx")
    dir.mkdirs()
    val file = File(dir, "galvyx_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    file.absolutePath to uri
}.getOrNull()

private fun exportVisitPdf(context: Context, visit: SiteVisit, profile: CompanyProfile): File? = runCatching {
    val reportsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Galvyx/Reports")
    reportsDir.mkdirs()
    val safeName = "Galvyx_${visit.clientName}_${visit.projectName}_${visit.date}_${System.currentTimeMillis()}".replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifBlank { "Galvyx_Report" }
    val file = File(reportsDir, "$safeName.pdf")

    val document = PdfDocument()
    val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true }
    val headerPaint = Paint().apply { textSize = 16f; isFakeBoldText = true }
    val bodyPaint = Paint().apply { textSize = 11f }
    val footerPaint = Paint().apply { textSize = 9f }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create())
    var canvas = page.canvas
    var y = 48f

    fun footer() {
        canvas.drawText("Galvyx • ${visit.title.take(52)}", 42f, 770f, footerPaint)
        canvas.drawText("Page $pageNumber", 540f, 770f, footerPaint)
    }

    fun newPage() {
        footer()
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageNumber).create())
        canvas = page.canvas
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
    line("Job Type: ${visit.jobType}")
    line(visit.summary())

    section("Notes")
    if (visit.notes.isEmpty()) line("No notes captured.")
    visit.notes.forEachIndexed { index, note ->
        line("${index + 1}. ${note.title.ifBlank { note.category }}", bodyPaint.apply { isFakeBoldText = true })
        bodyPaint.isFakeBoldText = false
        line("Location: ${note.location} • Category: ${note.category}")
        wrapped(note.notes)
        y += 5f
    }

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

    section("Expenses")
    if (visit.expenses.isEmpty()) line("No expenses captured.")
    visit.expenses.forEachIndexed { index, expense ->
        line("${index + 1}. ${expense.vendor} ${expense.amount}".trim(), bodyPaint.apply { isFakeBoldText = true })
        bodyPaint.isFakeBoldText = false
        line("${expense.date} • ${expense.category} • ${expense.paymentMethod}")
        wrapped(expense.notes)
        y += 5f
    }
    if (visit.expenses.isNotEmpty()) line("Expense Total: ${visit.expenses.sumOf { it.amount.toMoneyOrZero() }.toCurrencyString()}", headerPaint, 22f)

    section("Photos")
    if (visit.photos.isEmpty()) line("No photos captured.")
    visit.photos.forEachIndexed { index, photo ->
        line("${index + 1}. ${photo.caption.ifBlank { "Photo" }}")
        val bitmap = BitmapFactory.decodeFile(photo.path)
        if (bitmap != null) {
            if (y > 610f) newPage()
            val maxWidth = 220f
            val scale = maxWidth / bitmap.width
            val width = maxWidth
            val height = bitmap.height * scale
            canvas.drawBitmap(bitmap, null, android.graphics.RectF(42f, y, 42f + width, y + height.coerceAtMost(150f)), null)
            y += height.coerceAtMost(150f) + 12f
        } else {
            line(photo.path)
        }
    }

    footer()
    document.finishPage(page)
    file.outputStream().use { document.writeTo(it) }
    document.close()
    file
}.getOrNull()

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Galvyx report"))
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GalvyxTheme { HomeScreen(visitCount = 3) }
}
