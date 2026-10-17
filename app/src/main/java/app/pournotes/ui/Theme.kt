@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.pournotes.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The interface choices of the Settings screen, readable anywhere below the theme. */
val LocalPrefs = staticCompositionLocalOf { Prefs() }

/** Wallpaper colours (Material You) from Android 12 unless a palette is picked; before that, the PRONOTE teal. */
@Composable
fun PournotesTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val context = LocalContext.current
    val scheme = remember(dark, prefs.palette, prefs.black, context) {
        val base = SEEDS.getOrNull(prefs.palette)?.let { schemeFrom(it, dark) }
            ?: if (Build.VERSION.SDK_INT >= 31) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
            else schemeFrom(SEEDS[1]!!, dark)
        if (dark && prefs.black) base.pureBlack() else base
    }
    val density = LocalDensity.current
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = if (prefs.expressive) MotionScheme.expressive() else MotionScheme.standard(),
        typography = remember(prefs.font) { appType(FONTS[prefs.font]) },
        shapes = remember(prefs.corners) { appShapes(CORNER_SCALE[prefs.corners]) },
    ) {
        // Text size scales on top of the system's own setting.
        CompositionLocalProvider(
            LocalPrefs provides prefs,
            LocalDensity provides Density(density.density, density.fontScale * TEXT_SCALE[prefs.textScale]),
            content = content,
        )
    }
}

/** Hue angles (CIE LCh) of the palettes, in the order of [PALETTES]; null = wallpaper. */
val SEEDS = listOf(null, 190f, 285f, 250f, 140f, 75f, 30f, 350f)
val PALETTES = listOf("Fond d'écran", "PRONOTE", "Indigo", "Bleu", "Vert", "Ambre", "Corail", "Rose")
val FONTS = listOf(FontFamily.Default, FontFamily.Serif, FontFamily.Monospace)
private val CORNER_SCALE = listOf(0.4f, 1f, 1.45f)
private val TEXT_SCALE = listOf(0.9f, 1f, 1.12f, 1.25f)

/** Black background for OLED screens; each container moves down one step so cards still stand out. */
private fun ColorScheme.pureBlack() = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = surfaceContainerLowest, surfaceContainer = surfaceContainerLow,
    surfaceContainerHigh = surfaceContainer, surfaceContainerHighest = surfaceContainerHigh,
)

/** A colour of tone [l] (L*, 0 black to 100 white), [chroma] and [hue]: what M3 tonal palettes are made of. */
private fun tone(l: Float, chroma: Float, hue: Float): Color {
    // Strong chroma does not fit in sRGB near black and white: it fades there, as in Material's palettes.
    val c = chroma * (1f - 0.7f * (kotlin.math.abs(l - 50f) / 50f))
    val r = Math.toRadians(hue.toDouble())
    return Color(l, (c * kotlin.math.cos(r)).toFloat(), (c * kotlin.math.sin(r)).toFloat(), colorSpace = ColorSpaces.CieLab).convert(ColorSpaces.Srgb)
}

/** M3's "vibrant" variant from one hue: a punchy primary, a tinted secondary, a tertiary turned 60° for contrast. */
internal fun schemeFrom(hue: Float, dark: Boolean): ColorScheme {
    fun p(t: Int) = tone(t.toFloat(), 56f, hue)
    fun s(t: Int) = tone(t.toFloat(), 24f, hue + 15f)
    fun t(t: Int) = tone(t.toFloat(), 40f, hue + 60f)
    fun n(t: Float) = tone(t, 5f, hue)
    fun v(t: Int) = tone(t.toFloat(), 10f, hue)
    fun e(t: Int) = tone(t.toFloat(), 60f, 30f)
    return if (!dark) lightColorScheme(
        primary = p(40), onPrimary = p(100), primaryContainer = p(90), onPrimaryContainer = p(10), inversePrimary = p(80),
        secondary = s(40), onSecondary = s(100), secondaryContainer = s(90), onSecondaryContainer = s(10),
        tertiary = t(40), onTertiary = t(100), tertiaryContainer = t(90), onTertiaryContainer = t(10),
        error = e(40), onError = e(100), errorContainer = e(90), onErrorContainer = e(10),
        background = n(98f), onBackground = n(10f), surface = n(98f), onSurface = n(10f),
        surfaceVariant = v(90), onSurfaceVariant = v(30), surfaceTint = p(40),
        inverseSurface = n(20f), inverseOnSurface = n(95f), outline = v(50), outlineVariant = v(80),
        surfaceBright = n(98f), surfaceDim = n(87f), surfaceContainerLowest = n(100f), surfaceContainerLow = n(96f),
        surfaceContainer = n(94f), surfaceContainerHigh = n(92f), surfaceContainerHighest = n(90f),
    ) else darkColorScheme(
        primary = p(80), onPrimary = p(20), primaryContainer = p(30), onPrimaryContainer = p(90), inversePrimary = p(40),
        secondary = s(80), onSecondary = s(20), secondaryContainer = s(30), onSecondaryContainer = s(90),
        tertiary = t(80), onTertiary = t(20), tertiaryContainer = t(30), onTertiaryContainer = t(90),
        error = e(80), onError = e(20), errorContainer = e(30), onErrorContainer = e(90),
        background = n(6f), onBackground = n(90f), surface = n(6f), onSurface = n(90f),
        surfaceVariant = v(30), onSurfaceVariant = v(80), surfaceTint = p(80),
        inverseSurface = n(90f), inverseOnSurface = n(20f), outline = v(60), outlineVariant = v(30),
        surfaceBright = n(24f), surfaceDim = n(6f), surfaceContainerLowest = n(4f), surfaceContainerLow = n(10f),
        surfaceContainer = n(12f), surfaceContainerHigh = n(17f), surfaceContainerHighest = n(22f),
    )
}

/** The M3 type scale with weight carrying the hierarchy: figures and headings heavy, metadata left regular. */
private fun appType(font: FontFamily) = Typography().run {
    fun TextStyle.w(weight: FontWeight? = fontWeight) = copy(fontFamily = font, fontWeight = weight)
    copy(
        displayLarge = displayLarge.w(FontWeight.ExtraBold), displayMedium = displayMedium.w(FontWeight.ExtraBold),
        displaySmall = displaySmall.w(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.w(FontWeight.ExtraBold), headlineMedium = headlineMedium.w(FontWeight.ExtraBold),
        headlineSmall = headlineSmall.w(FontWeight.Bold),
        titleLarge = titleLarge.w(FontWeight.Bold), titleMedium = titleMedium.w(FontWeight.SemiBold),
        titleSmall = titleSmall.w(), bodyLarge = bodyLarge.w(), bodyMedium = bodyMedium.w(), bodySmall = bodySmall.w(),
        labelLarge = labelLarge.w(FontWeight.SemiBold), labelMedium = labelMedium.w(), labelSmall = labelSmall.w(),
    )
}

/** Soft containers (24–28 dp, the Breezy Weather cards) against pill-shaped chips and badges. */
private fun appShapes(k: Float) = Shapes(
    extraSmall = RoundedCornerShape(6.dp * k),
    small = RoundedCornerShape(12.dp * k),
    medium = RoundedCornerShape(16.dp * k),
    large = RoundedCornerShape(24.dp * k),
    extraLarge = RoundedCornerShape(28.dp * k),
)

/** A rounded shape whose top and bottom corners spring to those of [top] and [bottom]: corners morph instead of jumping. */
@Composable
fun morphShape(top: CornerBasedShape, bottom: CornerBasedShape = top): Shape {
    val density = LocalDensity.current
    val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val t by animateFloatAsState(top.topStart.toPx(ANY_SIZE, density), spec, label = "top")
    val b by animateFloatAsState(bottom.bottomStart.toPx(ANY_SIZE, density), spec, label = "bottom")
    // The spring overshoots a little: never below a square corner.
    return RoundedCornerShape(t.coerceAtLeast(0f), t.coerceAtLeast(0f), b.coerceAtLeast(0f), b.coerceAtLeast(0f))
}

private val ANY_SIZE = Size(10_000f, 10_000f)

/** [rest], tightening to [pressed] under the finger. */
@Composable
fun pressShape(source: InteractionSource, rest: CornerBasedShape, pressed: CornerBasedShape): Shape {
    val down by source.collectIsPressedAsState()
    return morphShape(if (down) pressed else rest)
}

/** Item [i] of [n] in an Expressive grouped list: large outer corners, tight inner ones; a pressed item rounds out. */
@Composable
fun groupShape(i: Int, n: Int, source: InteractionSource): Shape {
    val s = MaterialTheme.shapes
    val down by source.collectIsPressedAsState()
    return morphShape(if (i == 0 || down) s.large else s.extraSmall, if (i == n - 1 || down) s.large else s.extraSmall)
}

// Material Symbols paths (Apache-2.0), drawn as vectors rather than shipping an icon library.
private fun icon(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
    .build()

object Icons {
    val school = icon("school", "M5,13.18v4L12,21l7,-3.82v-4L12,17l-7,-3.82zM12,3L1,9l11,6 9,-4.91V17h2V9L12,3z")
    val link = icon("link", "M3.9,12c0,-1.71 1.39,-3.1 3.1,-3.1h4V7H7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5h4v-1.9H7c-1.71,0 -3.1,-1.39 -3.1,-3.1zM8,13h8v-2H8v2zM17,7h-4v1.9h4c1.71,0 3.1,1.39 3.1,3.1s-1.39,3.1 -3.1,3.1h-4V17h4c2.76,0 5,-2.24 5,-5s-2.24,-5 -5,-5z")
    val person = icon("person", "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z")
    val eye = icon("eye", "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zm0,-8c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z")
    val eyeOff = icon("eyeOff", "M12,7c2.76,0 5,2.24 5,5 0,0.65 -0.13,1.26 -0.36,1.83l2.92,2.92c1.51,-1.26 2.7,-2.89 3.43,-4.75 -1.73,-4.39 -6,-7.5 -11,-7.5 -1.4,0 -2.74,0.25 -3.98,0.7l2.16,2.16C10.74,7.13 11.35,7 12,7zM2,4.27l2.28,2.28 0.46,0.46C3.08,8.3 1.78,10.02 1,12c1.73,4.39 6,7.5 11,7.5 1.55,0 3.03,-0.3 4.38,-0.84l0.42,0.42L19.73,22 21,20.73 3.27,3 2,4.27zM7.53,9.8l1.55,1.55c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.66 1.34,3 3,3 0.22,0 0.44,-0.03 0.65,-0.08l1.55,1.55c-0.67,0.33 -1.41,0.53 -2.2,0.53 -2.76,0 -5,-2.24 -5,-5 0,-0.79 0.2,-1.53 0.53,-2.2zM11.84,9.02l3.15,3.15 0.02,-0.16c0,-1.66 -1.34,-3 -3,-3l-0.17,0.01z")
    val logout = icon("logout", "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5zM4,5h8V3H4c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h8v-2H4V5z")
    val clock = icon("clock", "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8zM12.5,7H11v6l5.25,3.15 0.75,-1.23 -4.5,-2.67z")
    val place = icon("place", "M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z")
    val book = icon("book", "M18,2H6c-1.1,0 -2,0.9 -2,2v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM6,4h5v8l-2.5,-1.5L6,12V4z")
    val star = icon("star", "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z")
    val check = icon("check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z")
    val warning = icon("warning", "M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z")
    val qr = icon("qr", "M9.5,6.5v3h-3v-3H9.5M11,5H5v6h6V5L11,5zM9.5,14.5v3h-3v-3H9.5M11,13H5v6h6V13L11,13zM17.5,6.5v3h-3v-3H17.5M19,5h-6v6h6V5L19,5zM13,13h1.5v1.5H13V13zM14.5,14.5H16V16h-1.5V14.5zM16,13h1.5v1.5H16V13zM13,16h1.5v1.5H13V16zM14.5,17.5H16V19h-1.5V17.5zM16,16h1.5v1.5H16V16zM17.5,14.5H19V16h-1.5V14.5zM17.5,17.5H19V19h-1.5V17.5zM22,7h-2V4h-3V2h5V7zM22,22v-5h-2v3h-3v2H22zM2,22h5v-2H4v-3H2V22zM2,2v5h2V4h3V2H2z")
    val calendar = icon("calendar", "M19,4h-1V2h-2v2H8V2H6v2H5c-1.11,0 -1.99,0.9 -1.99,2L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2zM19,20H5V10h14v10zM9,14H7v-2h2v2zm4,0h-2v-2h2v2zm4,0h-2v-2h2v2zm-8,4H7v-2h2v2zm4,0h-2v-2h2v2zm4,0h-2v-2h2v2z")
    val settings = icon("settings", "M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z")
    val back = icon("back", "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z")
    val prev = icon("prev", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z")
    val next = icon("next", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    val palette = icon("palette", "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5 0,-4.42 -4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z")
    val home = icon("home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z")
    val refresh = icon("refresh", "M17.65,6.35C16.2,4.9 14.21,4 12,4c-4.42,0 -7.99,3.58 -7.99,8s3.57,8 7.99,8c3.73,0 6.84,-2.55 7.73,-6h-2.08c-0.82,2.33 -3.04,4 -5.65,4 -3.31,0 -6,-2.69 -6,-6s2.69,-6 6,-6c1.66,0 3.14,0.69 4.22,1.78L13,11h7V4l-2.35,2.35z")
}
