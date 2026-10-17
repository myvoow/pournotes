@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package app.pournotes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@Composable
fun LoginScreen(
    busy: Boolean,
    serverError: String?,
    onLogin: (url: String, user: String, pass: String, remember: Boolean) -> Unit,
    onQr: (qr: String, pin: String, remember: Boolean) -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }
    var keep by rememberSaveable { mutableStateOf(true) }
    val valid = url.isNotBlank() && user.isNotBlank() && pass.isNotEmpty()
    val submit = { if (valid && !busy) onLogin(url, user, pass, keep) }
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var scanned by rememberSaveable { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    val error = scanError ?: serverError
    // Google's scanner runs in Play services: no camera permission, no camera UI to maintain.
    val scan = {
        scanError = null
        GmsBarcodeScanning.getClient(context, GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
            .startScan()
            .addOnSuccessListener { scanned = it.rawValue }
            .addOnFailureListener { scanError = "Scanner indisponible : ${it.message}" }
        Unit
    }
    scanned?.let { qr -> PinDialog(onDismiss = { scanned = null }) { pin -> scanned = null; onQr(qr, pin, keep) } }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Hero: a scalloped badge, then the name at display size.
            Surface(Modifier.padding(top = 32.dp).size(96.dp), shape = MaterialShapes.Cookie9Sided.toShape(), color = scheme.primaryContainer) {
                Icon(Icons.school, null, Modifier.padding(26.dp))
            }
            Text("Pournotes", style = MaterialTheme.typography.displayMedium)
            Text(
                "Emploi du temps, devoirs et notes de ton espace PRONOTE.",
                style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant,
            )

            // The quickest way in: one scan, four digits.
            FilledTonalButton(
                onClick = scan,
                enabled = !busy,
                shapes = ButtonDefaults.shapes(pressedShape = ButtonDefaults.largePressedShape),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Icon(Icons.qr, null)
                Text("Se connecter avec un QR code", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Sur PRONOTE (web) : Mes données › Compte › Connexion à l'application mobile, choisis un code à 4 chiffres puis scanne.",
                style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
            )
            Text("ou avec ton identifiant", Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)

            BentoCard(scheme.surfaceContainerLow) {
                OutlinedTextField(
                    url, { url = it }, Modifier.fillMaxWidth(),
                    label = { Text("Adresse PRONOTE") },
                    placeholder = { Text("https://….index-education.net/pronote/") },
                    leadingIcon = { Icon(Icons.link, null) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    user, { user = it }, Modifier.fillMaxWidth(),
                    label = { Text("Identifiant") },
                    leadingIcon = { Icon(Icons.person, null) },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    pass, { pass = it }, Modifier.fillMaxWidth(),
                    label = { Text("Mot de passe") },
                    singleLine = true, shape = MaterialTheme.shapes.medium,
                    visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton({ shown = !shown }) {
                            Icon(if (shown) Icons.eyeOff else Icons.eye, if (shown) "Masquer le mot de passe" else "Afficher le mot de passe")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Rester connecté", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(keep, { keep = it })
                }
            }

            AnimatedVisibility(
                error != null,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Surface(shape = MaterialTheme.shapes.large, color = scheme.errorContainer) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.warning, null)
                        Text(error.orEmpty(), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // The screen's one action: 64 dp tall, rounding into a squarer shape when pressed.
            Button(
                onClick = submit,
                enabled = valid || busy,
                shapes = ButtonDefaults.shapes(pressedShape = ButtonDefaults.largePressedShape),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Crossfade(busy, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "login") { b ->
                    if (b) LoadingIndicator(Modifier.size(44.dp), color = LocalContentColor.current)
                    else Text("Se connecter", style = MaterialTheme.typography.titleMedium)
                }
            }
            TextButton(
                onClick = {
                    url = "https://demo.index-education.net/pronote/"
                    user = "demonstration"
                    pass = "pronotevs"
                },
                enabled = !busy,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Remplir avec le compte de démonstration") }

            Text(
                "Connexion directe à PRONOTE uniquement : les connexions par ENT et les comptes parents arrivent plus tard. " +
                    "Application indépendante, sans lien avec Index Éducation.",
                style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** The 4-digit code chosen on PRONOTE when the QR code was made: it unlocks the code's contents. */
@Composable
private fun PinDialog(onDismiss: () -> Unit, onPin: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.qr, null) },
        title = { Text("Code à 4 chiffres") },
        text = {
            OutlinedTextField(
                pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                label = { Text("Code choisi sur PRONOTE") },
                singleLine = true, shape = MaterialTheme.shapes.medium,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (pin.length == 4) onPin(pin) }),
            )
        },
        confirmButton = { Button({ onPin(pin) }, enabled = pin.length == 4) { Text("Valider") } },
        dismissButton = { TextButton(onDismiss) { Text("Annuler") } },
    )
}
