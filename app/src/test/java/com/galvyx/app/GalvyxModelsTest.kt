package com.galvyx.app

import org.junit.Assert.assertEquals
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
            notes = listOf(VisitNote(location = "MDF", title = "Firewall", notes = "FCC listener checked")),
            devices = listOf(DeviceInfo(hostname = "sw-core", ipAddress = "10.0.31.31")),
            expenses = listOf(VisitExpense(vendor = "Fuel Stop", amount = "\$42.10")),
            photos = listOf(VisitPhoto(path = "/tmp/photo.jpg", caption = "Rack overview", category = "MDF", stage = "After"))
        )

        assertEquals(true, visit.matchesSearch("tahoe"))
        assertEquals(true, visit.matchesSearch("10.0.31.31"))
        assertEquals(true, visit.matchesSearch("rack overview"))
        assertEquals(true, visit.matchesSearch("mdf after"))
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
}
