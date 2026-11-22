package net.prototux.aare.ui.phone.next

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element

/** Review NA-1: only MyPlay's own intents may open a page by route. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NextDeepLinkTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun ourIntentOpensItsPage() =
        assertEquals("more/advanced/certificates", NextDeepLink.routeOf(NextDeepLink.intent(ctx, "more/advanced/certificates")))

    @Test fun anotherAppsIntentToTheLauncherIsIgnored() {
        val outside = Intent().setComponent(ComponentName(ctx.packageName, "net.prototux.aare.ui.MainActivity"))
            .putExtra(NextDeepLink.EXTRA_ROUTE, "more/backup")
        assertNull(NextDeepLink.routeOf(outside))
        assertNull(NextDeepLink.routeOf(Intent().putExtra(NextDeepLink.EXTRA_ROUTE, "more/backup")))
        assertNull(NextDeepLink.routeOf(null))
    }

    @Test fun theAliasIsNotExportedAndOpensMainActivity() {
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml").map(::File).first { it.isFile }
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder().parse(manifest)
        val ns = "http://schemas.android.com/apk/res/android"
        val aliases = doc.getElementsByTagName("activity-alias")
        val alias = (0 until aliases.length).map { aliases.item(it) as Element }
            .single { "net.prototux.aare" + it.getAttributeNS(ns, "name") == NextDeepLink.ALIAS }
        assertEquals("false", alias.getAttributeNS(ns, "exported"))
        assertEquals(".ui.MainActivity", alias.getAttributeNS(ns, "targetActivity"))
        assertEquals(0, alias.getElementsByTagName("intent-filter").length)
    }
}
