package com.ponto.ponto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Secao(val titulo: String, val icone: ImageVector, val campos: List<Campo>)

private val SECOES = listOf(
    Secao("Entrada", Icons.AutoMirrored.Filled.Login, listOf(Campo.ENTRADA_MIN, Campo.ENTRADA_MAX)),
    Secao(
        "Almoço",
        Icons.Filled.Restaurant,
        listOf(Campo.SAIDA_ALMOCO_MIN, Campo.SAIDA_ALMOCO_MAX, Campo.ALMOCO_MIN, Campo.ALMOCO_MAX, Campo.RETORNO_MIN),
    ),
    Secao("Jornada", Icons.Outlined.WorkOutline, listOf(Campo.JORNADA_SEMANA, Campo.JORNADA_SEXTA, Campo.VARIACAO)),
    Secao("Saída · segunda a quinta", Icons.AutoMirrored.Filled.Logout, listOf(Campo.SAIDA_MIN_SEMANA, Campo.SAIDA_MAX_SEMANA)),
    Secao("Saída · sexta", Icons.AutoMirrored.Filled.Logout, listOf(Campo.SAIDA_MIN_SEXTA, Campo.SAIDA_MAX_SEXTA)),
)

/**
 * O teclado numérico do Android não tem ":", então o campo guarda só os
 * dígitos ("0705") e mostra "07:05".
 */
private object HoraTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digitos = text.text
        val comDoisPontos = digitos.length > 2
        val exibido = if (comDoisPontos) digitos.take(2) + ":" + digitos.drop(2) else digitos
        val mapa = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (comDoisPontos && offset > 2) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = if (comDoisPontos && offset > 2) offset - 1 else offset
        }
        return TransformedText(AnnotatedString(exibido), mapa)
    }
}

private fun textoDoCampo(campo: Campo, cfg: Config): String {
    val valor = campo.ler(cfg)
    return if (campo.tipo == Tipo.HORA) hhmm(valor).replace(":", "") else valor.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaConfig(
    cfg: Config,
    snackbar: SnackbarHostState,
    onSalvar: (Config) -> Unit,
    onRestaurar: () -> Unit,
    onErro: () -> Unit,
    onVoltar: () -> Unit,
) {
    val textos = remember { mutableStateMapOf<Campo, String>().apply { Campo.entries.forEach { put(it, textoDoCampo(it, cfg)) } } }
    val erros = remember { mutableStateMapOf<Campo, String>() }
    var confirmarRestauracao by remember { mutableStateOf(false) }

    fun lerFormulario(): Config? {
        erros.clear()
        var novo = cfg
        for (campo in Campo.entries) {
            val texto = textos[campo].orEmpty()
            val valor = if (campo.tipo == Tipo.HORA) lerHhmm(texto) else texto.toIntOrNull()
            if (valor == null) {
                erros[campo] = if (campo.tipo == Tipo.HORA) "Use HH:MM" else "Número inválido"
            } else {
                novo = campo.gravar(novo, valor)
            }
        }
        if (erros.isEmpty()) erros.putAll(novo.validar())
        return if (erros.isEmpty()) novo else null
    }

    Scaffold(
        containerColor = Fundo,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Fundo),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Os valores ficam salvos neste celular. Nos horários, digite só os números (ex.: 0705).",
                fontSize = 13.sp,
                color = Cinza600,
            )

            SECOES.forEach { secao ->
                CartaoSecao(secao.titulo, secao.icone) {
                    LinhasDeCampos(secao.campos, textos, erros)
                }
            }

            Button(
                onClick = {
                    val novo = lerFormulario()
                    if (novo == null) onErro() else onSalvar(novo)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cor),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Salvar")
            }
            OutlinedButton(
                onClick = { confirmarRestauracao = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Vermelho),
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restaurar padrões")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmarRestauracao) {
        AlertDialog(
            onDismissRequest = { confirmarRestauracao = false },
            title = { Text("Restaurar padrões?") },
            text = { Text("Todas as configurações voltam para os valores originais do app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestauracao = false
                    onRestaurar()
                    Campo.entries.forEach { textos[it] = textoDoCampo(it, Config.PADRAO) }
                    erros.clear()
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRestauracao = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun CartaoSecao(titulo: String, icone: ImageVector, conteudo: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().cartao().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, contentDescription = null, tint = Cor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Cinza900)
        }
        conteudo()
    }
}

/** Campos dois a dois; um campo sozinho fica com meia largura, como os outros. */
@Composable
private fun LinhasDeCampos(
    campos: List<Campo>,
    textos: SnapshotStateMap<Campo, String>,
    erros: SnapshotStateMap<Campo, String>,
) {
    campos.chunked(2).forEach { par ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            par.forEach { campo ->
                val hora = campo.tipo == Tipo.HORA
                OutlinedTextField(
                    value = textos[campo].orEmpty(),
                    onValueChange = { novo ->
                        textos[campo] = novo.filter(Char::isDigit).take(if (hora) 4 else 3)
                        erros.remove(campo)
                    },
                    label = { Text(campo.rotulo) },
                    placeholder = if (hora) ({ Text("HH:MM") }) else null,
                    suffix = if (hora) null else ({ Text("min") }),
                    visualTransformation = if (hora) HoraTransformation else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = campo in erros,
                    supportingText = erros[campo]?.let { { Text(it) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            }
            if (par.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
