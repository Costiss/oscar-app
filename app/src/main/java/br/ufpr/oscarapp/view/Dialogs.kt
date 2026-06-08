package br.ufpr.oscarapp.view

import android.content.Context
import br.ufpr.oscarapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Small helpers so every screen shows feedback consistently. */

fun Context.mostrarErro(mensagem: String) {
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dlg_erro)
        .setMessage(mensagem)
        .setPositiveButton(R.string.dlg_ok, null)
        .show()
}

fun Context.mostrarSucesso(mensagem: String, onOk: () -> Unit = {}) {
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dlg_sucesso)
        .setMessage(mensagem)
        .setCancelable(false)
        .setPositiveButton(R.string.dlg_ok) { _, _ -> onOk() }
        .show()
}
