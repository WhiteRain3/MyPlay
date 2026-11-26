package net.prototux.aare.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.CarVideo

/**
 * MyPlay (VA-5): "Play on car" in the phone's share sheet. A shared video link (YouTube's Share, or
 * any app's) opens in the real app on the car screen, through [CarRuntime.openVideo]; never the
 * phone's screen. Only while a car is connected; nothing is kept. No window of its own: a toast
 * says how it went.
 */
class PlayOnCarActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            else -> null
        }
        val link = CarVideo.linkIn(text)
        val runtime = CarRuntime.active?.takeIf { it.isCar }
        val said = when {
            link == null -> R.string.myplay_play_on_car_no_link
            runtime == null -> R.string.myplay_play_on_car_no_car
            runtime.openVideo(link) -> R.string.myplay_play_on_car_opening
            else -> R.string.myplay_play_on_car_no_app
        }
        Toast.makeText(applicationContext, said, Toast.LENGTH_SHORT).show()
        finish()
    }
}
