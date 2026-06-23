package com.controller;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class WhatsAppSender {

    public static void enviarMensagem(Context context, String mensagem) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            // Converte o texto para o formato de link do WhatsApp
            String url = "https://api.whatsapp.com/send?text=" + Uri.encode(mensagem);
            intent.setData(Uri.parse(url));

            // Dispara a ação de abrir o aplicativo
            context.startActivity(intent);
        } catch (Exception e) {
            // Caso o WhatsApp não esteja instalado no aparelho
            Toast.makeText(context, "Erro: WhatsApp não encontrado neste dispositivo.", Toast.LENGTH_LONG).show();
        }
    }
}
