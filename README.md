# Ponto

App Android feito em Python com [Flet](https://flet.dev) que gera horários de ponto aleatórios: Entrada – Saída almoço – Retorno – Saída.

## Regras

- **Entrada:** entre 06:55 e 07:15.
- **Saída almoço:** 12:00 ± 10 min.
- **Almoço:** de 1h00 a 1h10.
- **Saída:** fecha 9h trabalhadas de segunda a quinta e 8h na sexta, com variação de ± 10 min (sem contar o almoço).

## Rodar e gerar o APK

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
