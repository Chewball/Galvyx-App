package com.galvyx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalvyxModelsTest {
    @Test
    fun siteVisitSummaryCountsCapturedItems() {
        val visit = SiteVisit(
            notes = listOf(VisitNote(title = "Rack"), VisitNote(title = "MDF")),
            devices = listOf(DeviceInfo(hostname = "switch-1")),
            expenses = listOf(VisitExpense(vendor = "Fuel")),
            photos = listOf(VisitPhoto(path = "/tmp/photo.jpg"), VisitPhoto(path = "/tmp/receipt.jpg"))
        )

        assertEquals("2 notes • 1 devices • 1 expenses • 2 photos", visit.summary())
    }

    @Test
    fun visitsJsonRoundTripPreservesCoreVisitDetails() {
        val visits = listOf(
            SiteVisit(
                clientName = "Reset",
                projectName = "Lake Tahoe Network Survey",
                technicianName = "Omar",
                date = "2026-08-21",
                jobType = "Network Survey",
                clientType = "Hotel / Hospitality",
                coreSystems = CoreSystems(emailPlatform = "Microsoft 365", emailDomain = "reset.example", mfaStatus = "Enabled", firewallPlatform = "SonicWall", wifiPlatform = "UniFi", rmmPlatform = "Atera"),
                notes = listOf(VisitNote(location = "MDF", category = "Switch / Firewall", title = "Firewall", notes = "FCC listener checked")),
                devices = listOf(DeviceInfo(location = "MDF", deviceType = "Firewall", manufacturer = "SonicWall", ipAddress = "10.0.31.31")),
                expenses = listOf(VisitExpense(date = "2026-08-21", category = "Meal", vendor = "Cafe", amount = "18.50", receiptPhotoPaths = listOf("/storage/receipt-1.jpg", "content://receipt-2"))),
                photos = listOf(VisitPhoto(path = "/storage/photo.jpg", caption = "MDF rack", category = "MDF", stage = "Before", isKeyPhoto = true))
            )
        )

        val restored = visitsFromJson(visitsToJson(visits))

        assertEquals(1, restored.size)
        assertEquals("Reset", restored.first().clientName)
        assertEquals("Lake Tahoe Network Survey", restored.first().projectName)
        assertEquals("Network Survey", restored.first().jobType)
        assertEquals("Hotel / Hospitality", restored.first().clientTypeLabel)
        assertEquals("Microsoft 365", restored.first().coreSystems.emailPlatform)
        assertEquals("reset.example", restored.first().coreSystems.emailDomain)
        assertEquals("SonicWall", restored.first().coreSystems.firewallPlatform)
        assertEquals("Firewall", restored.first().notes.first().title)
        assertEquals("10.0.31.31", restored.first().devices.first().ipAddress)
        assertEquals("18.50", restored.first().expenses.first().amount)
        assertEquals(listOf("/storage/receipt-1.jpg", "content://receipt-2"), restored.first().expenses.first().receiptPhotoPaths)
        assertEquals("2 receipt scans", restored.first().expenses.first().receiptCountLabel)
        assertEquals("MDF rack", restored.first().photos.first().caption)
        assertEquals("MDF", restored.first().photos.first().category)
        assertEquals("Before", restored.first().photos.first().stage)
        assertEquals(true, restored.first().photos.first().isKeyPhoto)
        assertEquals(0, restored.first().photos.first().rotationDegrees)
    }

    @Test
    fun photoRotationPersistsAndNormalizes() {
        val visits = listOf(
            SiteVisit(
                photos = listOf(
                    VisitPhoto(path = "/storage/sideways.jpg", caption = "Rack", rotationDegrees = 450)
                )
            )
        )

        val restored = visitsFromJson(visitsToJson(visits))

        assertEquals(90, restored.first().photos.first().rotationDegrees)
        assertEquals(270, (-90).normalizedRotationDegrees())
    }

    @Test
    fun moneyParserHandlesSymbolsCommasAndInvalidValues() {
        assertEquals(1234.56, "$1,234.56".toMoneyOrZero(), 0.001)
        assertEquals(18.5, "18.50".toMoneyOrZero(), 0.001)
        assertEquals(0.0, "not a number".toMoneyOrZero(), 0.001)
    }

    @Test
    fun currencyFormatterUsesTwoDecimalPlaces() {
        assertEquals("$18.50", 18.5.toCurrencyString())
        assertEquals("$0.00", 0.0.toCurrencyString())
    }

    @Test
    fun visitSearchMatchesNestedFieldData() {
        val visit = SiteVisit(
            clientName = "Reset",
            projectName = "Tahoe Refresh",
            technicianName = "Omar",
            jobType = "Network Survey",
            coreSystems = CoreSystems(emailPlatform = "Google Workspace", emailDomain = "example.com", securityPlatform = "Huntress"),
            notes = listOf(VisitNote(location = "MDF", title = "Firewall", notes = "FCC listener checked")),
            devices = listOf(DeviceInfo(hostname = "sw-core", ipAddress = "10.0.31.31")),
            expenses = listOf(VisitExpense(vendor = "Fuel Stop", amount = "\$42.10")),
            photos = listOf(VisitPhoto(path = "/tmp/photo.jpg", caption = "Rack overview", category = "MDF", stage = "After"))
        )

        assertEquals(true, visit.matchesSearch("tahoe"))
        assertEquals(true, visit.matchesSearch("10.0.31.31"))
        assertEquals(true, visit.matchesSearch("rack overview"))
        assertEquals(true, visit.matchesSearch("mdf after"))
        assertEquals(true, visit.matchesSearch("google workspace"))
        assertEquals(true, visit.matchesSearch("huntress"))
        assertEquals(false, visit.matchesSearch("unknown-client"))
    }


    @Test
    fun expenseBreakdownCalculatesTotalsByCategoryPaymentAndReceiptStatus() {
        val visit = SiteVisit(
            expenses = listOf(
                VisitExpense(category = "Fuel", vendor = "Shell", amount = "\$45.25", paymentMethod = "Reimbursable", receiptPhotoPaths = listOf("/receipt/fuel.jpg")),
                VisitExpense(category = "Meal", vendor = "Cafe", amount = "18.50", paymentMethod = "Company Card"),
                VisitExpense(category = "Fuel", vendor = "Chevron", amount = "12.25", paymentMethod = "Reimbursable", receiptPhotoPaths = listOf("/receipt/fuel2a.jpg", "/receipt/fuel2b.jpg"))
            )
        )

        val breakdown = visit.expenseBreakdown

        assertEquals(76.0, breakdown.total, 0.001)
        assertEquals(57.5, breakdown.reimbursableTotal, 0.001)
        assertEquals(18.5, breakdown.companyPaidTotal, 0.001)
        assertEquals(57.5, breakdown.receiptAttachedTotal, 0.001)
        assertEquals(18.5, breakdown.missingReceiptTotal, 0.001)
        assertEquals(3, breakdown.receiptScanCount)
        assertEquals(1, breakdown.missingReceiptCount)
        assertEquals("Fuel", breakdown.byCategory.first().label)
        assertEquals(57.5, breakdown.byCategory.first().total, 0.001)
        assertEquals("Reimbursable", breakdown.byPaymentMethod.first().label)
    }

    @Test
    fun pdfExportOptionsHideCoreSystemsByDefault() {
        assertEquals(false, PdfExportOptions().includeCoreSystems)
        assertEquals(true, PdfExportOptions(includeCoreSystems = true).includeCoreSystems)
    }

    @Test
    fun companyProfileJsonRoundTripPreservesStorageSettings() {
        val profile = CompanyProfile(
            companyName = "Acme Field Services",
            technicianName = "Omar",
            reportFooter = "Prepared by Galvyx",
            storageMode = StorageMode.SharePoint,
            localRootFolder = "Field Reports",
            localReportsFolder = "PDF Exports",
            localPhotosFolder = "Photo Attachments",
            localReportsTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AReports",
            localPhotosTreeUri = "content://com.android.externalstorage.documents/tree/primary%3APhotos",
            backupTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AGalvyxBackups",
            autoBackupEnabled = true,
            sharePointSiteUrl = "https://contoso.sharepoint.com/sites/FieldOps",
            sharePointLibraryName = "Documents",
            sharePointFolderPath = "Galvyx/Site Visits"
        )

        val restored = CompanyProfile.fromJson(profile.toJson())

        assertEquals(StorageMode.SharePoint, restored.storageMode)
        assertEquals("Field Reports", restored.localRootFolder)
        assertEquals("PDF Exports", restored.localReportsFolder)
        assertEquals("Photo Attachments", restored.localPhotosFolder)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3AReports", restored.localReportsTreeUri)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3APhotos", restored.localPhotosTreeUri)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3AGalvyxBackups", restored.backupTreeUri)
        assertEquals(true, restored.autoBackupEnabled)
        assertEquals("https://contoso.sharepoint.com/sites/FieldOps", restored.sharePointSiteUrl)
        assertEquals("Documents", restored.sharePointLibraryName)
        assertEquals("Galvyx/Site Visits", restored.sharePointFolderPath)
    }

    @Test
    fun backupJsonRoundTripPreservesProfileAndVisits() {
        val profile = CompanyProfile(companyName = "Acme", technicianName = "Omar", localReportsFolder = "Reports")
        val visits = listOf(
            SiteVisit(
                clientName = "Reset",
                projectName = "Tahoe",
                notes = listOf(VisitNote(title = "MDF")),
                photos = listOf(VisitPhoto(path = "content://photo", caption = "Rack"))
            )
        )

        val restored = backupFromJson(backupToJson(profile, visits))

        assertEquals("Acme", restored?.profile?.companyName)
        assertEquals("Omar", restored?.profile?.technicianName)
        assertEquals(1, restored?.visits?.size)
        assertEquals("Reset", restored?.visits?.first()?.clientName)
        assertEquals("MDF", restored?.visits?.first()?.notes?.first()?.title)
        assertEquals("Rack", restored?.visits?.first()?.photos?.first()?.caption)
    }

    @Test
    fun hotelAuditItemsRoundTripWithStableIdsAndStatuses() {
        val item = HotelAuditItem(
            id = "audit-123",
            building = "Tower A",
            floor = "3",
            roomArea = "Room 301",
            category = "Guest Rooms",
            requiredEvidence = "Photograph each room type and document network/TV/phone endpoints",
            status = HotelAuditStatus.INACCESSIBLE,
            notes = "Occupied during audit"
        )

        val restored = visitsFromJson(visitsToJson(listOf(SiteVisit(hotelAuditItems = listOf(item))))).single().hotelAuditItems.single()

        assertEquals(item, restored)
        assertEquals("Inaccessible", restored.status.label)
    }

    @Test
    fun oldVisitJsonWithoutHotelAuditItemsRemainsCompatible() {
        val oldJson = """[{"id":"old-visit","clientName":"Legacy Hotel","clientType":"Hotel / Hospitality"}]"""

        val restored = visitsFromJson(oldJson).single()

        assertEquals("old-visit", restored.id)
        assertTrue(restored.hotelAuditItems.isEmpty())
        assertEquals(0, restored.hotelAuditCoverage.total)
        assertEquals(0, restored.hotelAuditCoverage.completionPercentage)
    }

    @Test
    fun siteVisitJsonNormalizesBlankHotelAuditItemIds() {
        val raw = """[{"hotelAuditItems":[{"id":"","roomArea":"Lobby"},{"id":"   ","roomArea":"MDF"}]}]"""

        val restored = visitsFromJson(raw).single().hotelAuditItems

        assertTrue(restored.all { it.id.isNotBlank() })
        assertEquals(restored.size, restored.map { it.id }.distinct().size)
    }

    @Test
    fun siteVisitJsonNormalizesDuplicateHotelAuditItemIdsAndPreservesUniqueIds() {
        val raw = """[{"hotelAuditItems":[{"id":"audit-1","roomArea":"Lobby"},{"id":"audit-1","roomArea":"MDF"},{"id":"audit-2","roomArea":"Roof"}]}]"""

        val restored = visitsFromJson(raw).single().hotelAuditItems
        val ids = restored.map { it.id }

        assertEquals(ids.size, ids.distinct().size)
        assertEquals("audit-1", restored[0].id)
        assertEquals("audit-2", restored[2].id)
    }

    @Test
    fun hotelAuditCoverageCountsOnlyCompleteAndKeepsAllGapsVisible() {
        val items = listOf(
            HotelAuditItem(roomArea = "Lobby", status = HotelAuditStatus.COMPLETE),
            HotelAuditItem(roomArea = "MDF", status = HotelAuditStatus.COMPLETE),
            HotelAuditItem(roomArea = "IDF 1", status = HotelAuditStatus.PENDING),
            HotelAuditItem(roomArea = "IDF 2", status = HotelAuditStatus.INACCESSIBLE),
            HotelAuditItem(roomArea = "Roof", status = HotelAuditStatus.DEFERRED)
        )

        val summary = SiteVisit(hotelAuditItems = items).hotelAuditCoverage

        assertEquals(5, summary.total)
        assertEquals(2, summary.complete)
        assertEquals(1, summary.pending)
        assertEquals(1, summary.inaccessible)
        assertEquals(1, summary.deferred)
        assertEquals(40, summary.completionPercentage)
        assertEquals(3, summary.unresolvedCount)
        assertEquals(listOf("IDF 1", "IDF 2", "Roof"), summary.unresolvedItems.map { it.roomArea })
    }

    @Test
    fun defaultHotelChecklistCoversRequiredPropertySystemsWithUsefulEvidence() {
        val checklist = defaultHotelAuditChecklist()
        val searchable = checklist.joinToString(" ") { "${it.roomArea} ${it.category} ${it.requiredEvidence}" }.lowercase()
        val requiredTerms = listOf(
            "exterior", "floor plan", "room list", "lobby", "front desk", "pms", "workstation",
            "restaurant", "bar", "kitchen", "pos", "mdf", "idf", "demarc", "primary circuit",
            "backup circuit", "low-voltage", "wi-fi", "guest room", "room type", "audio", "camera",
            "nvr", "phone", "pbx", "iptv", "signage", "access control", "door lock", "ups", "power",
            "inaccessible", "omitted"
        )

        assertTrue(checklist.isNotEmpty())
        assertTrue(checklist.all { it.id.isNotBlank() && it.roomArea.isNotBlank() && it.category.isNotBlank() && it.requiredEvidence.isNotBlank() })
        requiredTerms.forEach { term -> assertTrue("Missing checklist coverage for $term", searchable.contains(term)) }
    }

    @Test
    fun visitSearchIncludesHotelAuditLocationEvidenceNotesAndStatus() {
        val visit = SiteVisit(
            hotelAuditItems = listOf(
                HotelAuditItem(
                    building = "Cascade Wing",
                    floor = "Lower Level",
                    roomArea = "Locked IDF 7",
                    category = "Network / Wi-Fi",
                    requiredEvidence = "Capture switch labels and AP inventory",
                    status = HotelAuditStatus.DEFERRED,
                    notes = "Return with facilities escort"
                )
            )
        )

        assertTrue(visit.matchesSearch("cascade wing"))
        assertTrue(visit.matchesSearch("lower level"))
        assertTrue(visit.matchesSearch("switch labels"))
        assertTrue(visit.matchesSearch("facilities escort"))
        assertTrue(visit.matchesSearch("deferred"))
        assertFalse(visit.matchesSearch("ballroom projector"))
    }
}
