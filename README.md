# Ponto

App Android que gera horários de ponto aleatórios: Entrada – Saída almoço – Retorno – Saída.

A partir da versão 2.0.0 o app é feito em **Kotlin** com Jetpack Compose (pasta `kotlin/`), com APK de cerca de 1,3 MB. A versão original em Python com [Flet](https://flet.dev) (pasta `src/`) continua no repositório como referência.

## Regras padrão

Todos estes valores podem ser alterados na tela de **Configurações** (engrenagem no canto superior direito). As alterações ficam salvas no celular, e o botão **Restaurar padrões** volta para os valores abaixo.

- **Entrada:** entre 06:55 e 07:15.
- **Saída almoço:** entre 11:55 e 12:10.
- **Almoço:** de 1h00 a 1h10, então o retorno nunca fica antes de 12:55.
- **Saída:** fecha 9h trabalhadas de segunda a quinta e 8h na sexta, com variação de ± 10 min (sem contar o almoço). De segunda a quinta, a saída nunca fica antes de 16:55.
- **Saída mínima na sexta:** desligada por padrão; quando ligada, usa 15:55.

O app abre com a data de hoje. Tocando na data dá para escolher outro dia no calendário, e a jornada segue o dia escolhido. O botão "Voltar para hoje" retorna à data automática.

Os horários mínimos (06:55, 11:55, 12:55 e 16:55) seguem a orientação do RH: o ponto nunca é batido antes deles.

## Gerar o APK (Kotlin)

Precisa do JDK 17 e do Android SDK (o caminho do SDK vai em `kotlin/local.properties`, por exemplo `sdk.dir=C\:/Users/SEU_USUARIO/Android/sdk`).

```powershell
cd kotlin
.\gradlew testDebugUnitTest   # testes das regras
.\gradlew assembleRelease     # gera app\build\outputs\apk\release\app-release.apk
```

Para lançar uma versão nova, suba `versionCode` e `versionName` em `kotlin/app/build.gradle.kts`.

O APK é assinado com a chave de debug (`~/.android/debug.keystore`), a mesma usada pelo `flet build`. Por isso a versão em Kotlin instala por cima da versão em Python e traz as configurações salvas nela.

## Versão em Python (Flet)

```powershell
python -m venv .venv
.venv\Scripts\activate
pip install flet==1.0.3
flet run src\main.py   # testar no PC
flet build apk         # gera build\apk\ponto.apk
```

No Windows, o `flet build apk` exige o Modo de Desenvolvedor ativado.

O APK pronto está na aba **Releases**.

---

Criado por Alexsandre JR
