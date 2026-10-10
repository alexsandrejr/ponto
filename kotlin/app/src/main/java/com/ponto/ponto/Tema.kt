package com.ponto.ponto

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cor = Color(0xFF3949AB)
val Fundo = Color(0xFFE8EAF6)
val FundoBaixo = Color(0xFFC5CAE9)
val Vermelho = Color(0xFFD32F2F)

// Tons de Blue Grey do Material
val Cinza100 = Color(0xFFCFD8DC)
val Cinza400 = Color(0xFF78909C)
val Cinza500 = Color(0xFF607D8B)
val Cinza600 = Color(0xFF546E7A)
val Cinza700 = Color(0xFF455A64)
val Cinza900 = Color(0xFF263238)

private val esquema = lightColorScheme(
    primary = Cor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondaryContainer = Color(0xFFDEE0FF),
    background = Fundo,
    surface = Color.White,
    surfaceContainerHigh = Color(0xFFEEEFF9),
    surfaceContainerHighest = Color(0xFFE8E9F3),
    error = Vermelho,
)

@Composable
fun PontoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, content = conteudo)
}
