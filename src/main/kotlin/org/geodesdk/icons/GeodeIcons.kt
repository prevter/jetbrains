package org.geodesdk.icons

import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import javax.swing.Icon
import kotlin.math.min

private const val DEFAULT_ICON_SIZE = 16

object GeodeIcons {
    @JvmField
    val Geode: ()->Icon = {
        val icon = IconLoader.getIcon("/icons/geode.png", javaClass)
        val scale = min(DEFAULT_ICON_SIZE.toFloat() / icon.iconWidth, DEFAULT_ICON_SIZE.toFloat() / icon.iconHeight)
        IconUtil.scale(icon, null, scale)
    }

    @JvmField
    val GeometryDash: ()->Icon = {
        val icon = IconLoader.getIcon("/icons/geometry_dash.png", javaClass)
        val scale = min(DEFAULT_ICON_SIZE.toFloat() / icon.iconWidth, DEFAULT_ICON_SIZE.toFloat() / icon.iconHeight)

        IconUtil.scale(icon, null, scale)
    }
}