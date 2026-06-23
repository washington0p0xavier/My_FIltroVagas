package com.controller;

import android.content.Context;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.net.URL;

public class PdfProcessor {

    // Interface para avisar a tela principal quando o texto estiver pronto
    public interface PdfCallback {
        void onTextExtracted(String rawText);
        void onError(Exception e);
    }

    public static void extractTextFromUrl(Context context, String pdfUrl, PdfCallback callback) {
        // Inicializa o motor do PDFBox
        PDFBoxResourceLoader.init(context);

        // Rodando em segundo plano para não travar o aplicativo
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i("AppVagas", "Iniciando leitura do PDF no link: " + pdfUrl);

                    // 1. Abre a conexão com o link do PDF
                    URL url = new URL(pdfUrl);
                    InputStream inputStream = url.openStream();

                    // 2. Carrega o documento na memória
                    PDDocument document = PDDocument.load(inputStream);

                    // 3. Extrai o texto tentando preservar a separação de colunas
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true); // Garante a ordem de leitura correta
                    stripper.setWordSeparator("  "); // Usa dois espaços como separador de palavras para facilitar a detecção de colunas
                    String extractedText = stripper.getText(document);

                    // 4. Fecha o documento para liberar memória
                    document.close();
                    inputStream.close();

                    // 5. Devolve o texto extraído
                    callback.onTextExtracted(extractedText);

                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}
