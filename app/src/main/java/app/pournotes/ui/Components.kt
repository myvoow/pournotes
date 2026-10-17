package app.pournotes.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A Bento tile: a tonal container with soft 28 dp corners that tighten under the finger and grow with its content. */
@Composable
fun BentoCard(
    container: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val shape = pressShape(source, MaterialTheme.shapes.extraLarge, MaterialTheme.shapes.large)
    val inner: @Composable () -> Unit = {
        Column(
            Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
    if (onClick == null) Surface(modifier, shape = shape, color = container, content = inner)
    else Surface(onClick, modifier, shape = shape, color = container, interactionSource = source, content = inner)
}

/** A figure that must be read at a glance: small label, huge value. */
@Composable
fun StatTile(label: String, value: String, caption: String?, container: Color, modifier: Modifier = Modifier) {
    BentoCard(container, modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.displaySmall, maxLines = 1)
        if (caption != null) Text(caption, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Section title with its icon in a pill-shaped tonal badge. */
@Composable
fun SectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null, Modifier.padding(8.dp).size(20.dp))
        }
        Text(title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleLarge)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A status or metadata chip. */
@Composable
fun Pill(text: String, container: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(modifier, shape = CircleShape, color = container, contentColor = contentColorFor(container)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, Modifier.padding(end = 6.dp).size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** An icon + text line of metadata. */
@Composable
fun Meta(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp))
        Text(text, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One row of an Expressive grouped list (item [i] of [n]). */
@Composable
fun GroupItem(i: Int, n: Int, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick, modifier,
        shape = groupShape(i, n, source),
        color = MaterialTheme.colorScheme.surfaceContainer,
        interactionSource = source,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/** PRONOTE's own subject colour ("#RRGGBB"), or the theme's primary. */
@Composable
fun subjectColor(hex: String?): Color = if (!LocalPrefs.current.subjectColors) MaterialTheme.colorScheme.primary else
    hex?.removePrefix("#")?.toLongOrNull(16)?.takeIf { hex.length == 7 }?.let { Color(0xFF000000 or it) } ?: MaterialTheme.colorScheme.primary

@Composable
fun SubjectDot(hex: String?, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(subjectColor(hex), CircleShape))
}
