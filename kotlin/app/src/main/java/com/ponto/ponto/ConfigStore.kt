package com.ponto.ponto

import android.content.Context

/** Guarda a configuração no celular, no mesmo formato JSON da versão em Python. */
class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("ponto", Context.MODE_PRIVATE)

    // Onde a versão em Python (Flet/Flutter) salvava a configuração
    private val prefsFlet = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

    fun carregar(): Config {
        prefs.getString(CHAVE, null)?.let { return Config.deJson(it) }
        // Primeira abertura depois de atualizar da versão em Python: traz o que já estava salvo
        val antigo = prefsFlet.getString("flutter.$CHAVE", null) ?: return Config.PADRAO
        return Config.deJson(antigo).also(::salvar)
    }

    fun salvar(cfg: Config) {
        prefs.edit().putString(CHAVE, cfg.paraJson()).apply()
    }

    private companion object {
        const val CHAVE = "ponto.config"
    }
}
