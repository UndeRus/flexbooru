/*
 * Copyright (C) 2020. by onlymash <fiepi.dev@gmail.com>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.app

import android.app.Application
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.dispose
import coil.load
import com.google.android.material.color.DynamicColors
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import okhttp3.OkHttpClient
import onlymash.flexbooru.R
import onlymash.flexbooru.app.Settings.nightMode
import onlymash.flexbooru.okhttp.AndroidCookieJar
import onlymash.flexbooru.okhttp.CloudflareInterceptor
import onlymash.flexbooru.okhttp.ProgressInterceptor
import onlymash.flexbooru.okhttp.RequestHeaderInterceptor
import org.kodein.di.DI
import org.kodein.di.DIAware

class App : Application(), DIAware, ImageLoaderFactory {

    companion object {
        lateinit var app: App
        private const val CACHE_MAX_PERCENT = 0.2
    }

    override val di by DI.lazy {
        import(appModule(this@App))
    }

    private val drawerImageLoader = object : AbstractDrawerImageLoader() {
        override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.load(uri) {
                placeholder(ContextCompat.getDrawable(imageView.context, R.drawable.avatar_account))
            }
        }
        override fun cancel(imageView: ImageView) {
            imageView.dispose()
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        initial()
    }

    private fun initial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        DrawerImageLoader.init(drawerImageLoader)
    }

    override fun newImageLoader(): ImageLoader {
        val builder = OkHttpClient.Builder()
            .cookieJar(AndroidCookieJar)
            .addNetworkInterceptor(RequestHeaderInterceptor())
            .addInterceptor(ProgressInterceptor())
        if (Settings.isBypassWAF) {
            builder.addInterceptor(CloudflareInterceptor(this))
        }
        if (Settings.isDohEnable) {
            builder.dns(Settings.doh)
        }
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(CACHE_MAX_PERCENT)
                    .build()
            }
            .allowHardware(false)
            .okHttpClient(builder.build())
            .crossfade(true)
            .build()
    }
}