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
                notes = listOf(VisitNote(location = "MDF", category = "Switch / Firewall", title = "Firewall", notes = "FCC listener checked")),
                devices = listOf(DeviceInfo(location = "MDF", deviceType = "Firewall", manufacturer = "SonicWall", ipAddress = "10.0.31.31")),
                expenses = listOf(VisitExpense(date = "2026-08-21", category = "Meal", vendor = "Cafe", amount = "18.50")),
                photos = listOf(VisitPhoto(path = "/storage/photo.jpg", caption = "MDF rack"))
            )
        )

        val restored = visitsFromJson(visitsToJson(visits))

        assertEquals(1, restored.size)
        assertEquals("Reset", restored.first().clientName)
        assertEquals("Lake Tahoe Network Survey", restored.first().projectName)
        assertEquals("Network Survey", restored.first().jobType)
        assertEquals("Firewall", restored.first().notes.first().title)
        assertEquals("10.0.31.31", restored.first().devices.first().ipAddress)
        assertEquals("18.50", restored.first().expenses.first().amount)
        assertEquals("MDF rack", restored.first().photos.first().caption)
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
            photos = listOf(VisitPhoto(path = "/tmp/photo.jpg", caption = "Rack overview"))
        )

        assertEquals(true, visit.matchesSearch("tahoe"))
        assertEquals(true, visit.matchesSearch("10.0.31.31"))
        assertEquals(true, visit.matchesSearch("rack overview"))
        assertEquals(false, visit.matchesSearch("unknown-client"))
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
        assertEquals("https://contoso.sharepoint.com/sites/FieldOps", restored.sharePointSiteUrl)
        assertEquals("Documents", restored.sharePointLibraryName)
        assertEquals("Galvyx/Site Visits", restored.sharePointFolderPath)
    }
}
