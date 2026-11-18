package net.prototux.aare.headunit

import aap_protobuf.service.media.sink.message.VideoConfigurationOuterClass.VideoConfiguration
import net.prototux.aare.aap.PhoneIdentity
import net.prototux.aare.aap.cert.SelfSignedCertificate
import net.prototux.aare.aap.Transport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class ScreenMarginsTest {
    /** The main display's video configuration in what the head unit tells the phone about itself. */
    private fun mainVideo(options: HeadUnit.Options): VideoConfiguration {
        val g = SelfSignedCertificate.generate("test head unit")
        val transport = object : Transport {
            override val name = "test"
            override val input: InputStream = ByteArrayInputStream(ByteArray(0))
            override val output: OutputStream = ByteArrayOutputStream()
            override fun close() {}
        }
        val unit = HeadUnit(transport, PhoneIdentity(g.certificate, g.keyPair.private), options, NoListener)
        return unit.discovery().channelsList.first { it.hasMediaSinkService() && it.mediaSinkService.videoConfigsCount > 0 }
            .mediaSinkService.getVideoConfigs(0)
    }

    @Test fun noMarginsByDefault() {
        val v = mainVideo(HeadUnit.Options())
        assertEquals(0, v.widthMargin)
        assertEquals(0, v.heightMargin)
        assertFalse(v.hasUiConfig())
    }

    @Test fun plainMarginsReachTheServiceDescription() {
        val v = mainVideo(HeadUnit.Options(widthMargin = 80, heightMargin = 6))
        assertEquals(80, v.widthMargin)
        assertEquals(6, v.heightMargin)
        assertFalse(v.hasUiConfig())
    }

    @Test fun sideMarginsReachTheServiceDescription() {
        val v = mainVideo(HeadUnit.Options(margins = SideMargins(left = 0, top = 1, right = 80, bottom = 2)))
        assertTrue(v.uiConfig.hasMargins())
        assertEquals(0, v.uiConfig.margins.left)
        assertEquals(1, v.uiConfig.margins.top)
        assertEquals(80, v.uiConfig.margins.right)
        assertEquals(2, v.uiConfig.margins.bottom)
    }

    @Test fun clusterDisplayKeepsNoMargins() {
        val g = SelfSignedCertificate.generate("test head unit")
        val transport = object : Transport {
            override val name = "test"
            override val input: InputStream = ByteArrayInputStream(ByteArray(0))
            override val output: OutputStream = ByteArrayOutputStream()
            override fun close() {}
        }
        val unit = HeadUnit(transport, PhoneIdentity(g.certificate, g.keyPair.private), HeadUnit.Options(cluster = true, widthMargin = 80), NoListener)
        val videos = unit.discovery().channelsList.filter { it.hasMediaSinkService() && it.mediaSinkService.videoConfigsCount > 0 }
            .map { it.mediaSinkService.getVideoConfigs(0) }
        assertEquals(listOf(80, 0), videos.map { it.widthMargin })
    }

    @Test fun touchPanelIsTheVisiblePart() {
        fun panel(options: HeadUnit.Options): Pair<Int, Int>? {
            val g = SelfSignedCertificate.generate("test head unit")
            val transport = object : Transport {
                override val name = "test"
                override val input: InputStream = ByteArrayInputStream(ByteArray(0))
                override val output: OutputStream = ByteArrayOutputStream()
                override fun close() {}
            }
            val input = HeadUnit(transport, PhoneIdentity(g.certificate, g.keyPair.private), options, NoListener)
                .discovery().channelsList.first { it.hasInputSourceService() }.inputSourceService
            return input.touchscreenList.firstOrNull()?.let { it.width to it.height }
        }
        assertEquals(1280 to 720, panel(HeadUnit.Options()))
        assertEquals(1200 to 720, panel(HeadUnit.Options(widthMargin = 80)))
        assertNull(panel(HeadUnit.Options(widthMargin = 80, noTouch = true)))
    }

    /** Plain margins are split evenly, as the phone does; the odd pixel goes right / down. */
    @Test fun plainMarginsAreSplitEvenly() {
        assertEquals(VisibleArea(40, 0, 1200, 720), visibleArea(HeadUnit.Options(widthMargin = 80), 1280, 720))
        assertEquals(VisibleArea(1, 2, 1277, 715), visibleArea(HeadUnit.Options(widthMargin = 3, heightMargin = 5), 1280, 720))
    }

    /** Per-side margins replace the plain ones. */
    @Test fun sideMarginsReplacePlainOnes() {
        val o = HeadUnit.Options(widthMargin = 200, heightMargin = 100, margins = SideMargins(0, 0, 80, 0))
        assertEquals(VisibleArea(0, 0, 1200, 720), visibleArea(o, 1280, 720))
    }

    @Test fun parsesSideMargins() {
        assertEquals(SideMargins(0, 1, 80, 2), SideMargins.parse("0,1,80,2"))
        assertEquals(SideMargins(0, 1, 80, 2), SideMargins.parse(" 0, 1 ,80,2 "))
        assertNull(SideMargins.parse("80"))
        assertNull(SideMargins.parse("0,0,80"))
        assertNull(SideMargins.parse("0,0,-80,0"))
        assertNull(SideMargins.parse("a,b,c,d"))
    }

    @Test fun refusesMarginsThatLeaveNothing() {
        assertNull(marginProblem(HeadUnit.Options(widthMargin = 80), 1280, 720))
        assertNotNull(marginProblem(HeadUnit.Options(widthMargin = 1280), 1280, 720))
        assertNotNull(marginProblem(HeadUnit.Options(heightMargin = -1), 1280, 720))
        assertNotNull(marginProblem(HeadUnit.Options(margins = SideMargins(640, 0, 640, 0)), 1280, 720))
    }

    @Test fun touchesAreCountedFromTheVisiblePart() {
        val o = HeadUnit.Options(margins = SideMargins(100, 10, 0, 0))
        assertEquals(0 to 0, toPanel(o, 100, 10))
        assertEquals(50 to 5, toPanel(o, 150, 15))
        assertEquals(0 to 0, toPanel(o, 3, 3))
        assertEquals(1179 to 709, toPanel(o, 5000, 5000))
        assertEquals(640 to 360, toPanel(HeadUnit.Options(), 640, 360))
    }

    @Test fun pictureMarksTheHiddenPart() {
        val white = BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { color = java.awt.Color.WHITE; fillRect(0, 0, 1280, 720); dispose() }
        }
        assertSame(white, markHidden(white, HeadUnit.Options()))
        val marked = markHidden(white, HeadUnit.Options(margins = SideMargins(0, 0, 80, 0)))
        assertEquals(0xFFFFFF, marked.getRGB(600, 360) and 0xFFFFFF)      // visible: untouched
        assertTrue((marked.getRGB(1250, 360) and 0xFF) < 0x80)            // hidden: darkened
        assertEquals(0xFFFFFF, white.getRGB(1250, 360) and 0xFFFFFF)      // the frame itself is not changed
    }

    private object NoListener : HeadUnit.Listener {
        override fun status(text: String) {}
        override fun videoStarted(width: Int, height: Int) {}
        override fun video(data: ByteArray, offset: Int, length: Int) {}
        override fun videoCodecConfig(data: ByteArray, offset: Int, length: Int) {}
        override fun audioStart(stream: aap_protobuf.service.media.sink.message.AudioStreamTypeOuterClass.AudioStreamType,
            config: aap_protobuf.service.media.shared.message.AudioConfigurationOuterClass.AudioConfiguration) {}
        override fun audio(stream: aap_protobuf.service.media.sink.message.AudioStreamTypeOuterClass.AudioStreamType, data: ByteArray, offset: Int, length: Int) {}
        override fun audioStop(stream: aap_protobuf.service.media.sink.message.AudioStreamTypeOuterClass.AudioStreamType) {}
        override fun micOpened() {}
        override fun micClosed() {}
        override fun navigation(text: String) {}
        override fun nowPlaying(text: String) {}
        override fun closed(reason: String) {}
    }
}
