package com.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.IOException;

public class WebScraper {

    // Interface para devolver o link para a tela principal quando terminar
    public interface ScraperCallback {
        void onLinkFound(String pdfUrl);
        void onError(Exception e);
    }

    // O Android não permite buscar dados na internet na mesma linha da tela (Main Thread)
    // por isso usamos uma Thread separada em segundo plano (Background)
    public static void findPdfLink(String siteUrl, ScraperCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 1. Conecta ao site e baixa o HTML da página
                    Document doc = Jsoup.connect(siteUrl).get();

                    // 2. Busca pela tag de link <a> que aponta para um arquivo PDF
                    // Aqui usamos um seletor que busca qualquer link que termine com ".pdf"
                    Element linkElement = doc.select("a[href$=.pdf]").first();

                    if (linkElement != null) {
                        // 3. Captura a URL absoluta do link do PDF
                        String pdfUrl = linkElement.absUrl("href");
                        callback.onLinkFound(pdfUrl);
                    } else {
                        callback.onError(new Exception("Nenhum link de PDF encontrado nesta página."));
                    }

                } catch (IOException e) {
                    callback.onError(e);
                }
            }
        }).start();
    }
}