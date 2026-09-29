package br.com.medshare.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta da marca MedShare, a mesma do painel web e dos documentos.
val VerdeMedShare = Color(0xFF0B8C6E)
val VerdeClaro = Color(0xFFDCEFE6)
val VerdeMedio = Color(0xFF7FCBA4)
val Tinta = Color(0xFF1B2620)
val TintaSuave = Color(0xFF5E6E66)
val Fundo = Color(0xFFFBFCFA)
val Superficie = Color(0xFFF2F6F3)
val Linha = Color(0xFFD7E1DB)
val Alerta = Color(0xFFB4552D)
val AlertaClaro = Color(0xFFF6E5DC)

private val paletaClara = lightColorScheme(
    primary = VerdeMedShare,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = Tinta,
    secondary = VerdeMedio,
    onSecondary = Tinta,
    background = Fundo,
    onBackground = Tinta,
    surface = Color.White,
    onSurface = Tinta,
    surfaceVariant = Superficie,
    onSurfaceVariant = TintaSuave,
    outline = Linha,
    error = Alerta,
    onError = Color.White,
    errorContainer = AlertaClaro,
    onErrorContainer = Color(0xFF7D3A1E),
)

private val paletaEscura = darkColorScheme(
    primary = VerdeMedio,
    onPrimary = Color(0xFF07281F),
    primaryContainer = Color(0xFF0B4A3A),
    onPrimaryContainer = VerdeClaro,
    background = Color(0xFF101714),
    onBackground = Color(0xFFE6EDE9),
    surface = Color(0xFF17201C),
    onSurface = Color(0xFFE6EDE9),
    surfaceVariant = Color(0xFF1F2A25),
    onSurfaceVariant = Color(0xFFB4C2BA),
    outline = Color(0xFF3A4A42),
    error = Color(0xFFE89070),
    onError = Color(0xFF3A1508),
)

private val tipografia = Typography(
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun TemaMedShare(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) paletaEscura else paletaClara,
        typography = tipografia,
        content = conteudo,
    )
}
