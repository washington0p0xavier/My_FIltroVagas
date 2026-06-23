package com.example.my_filtrovagas;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Importe a classe que criamos. Ajuste o caminho se necessário.
import com.controller.WebScraper;
import com.example.my_filtrovagas.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Carrega o visual da tela (onde está o Hello World)
        setContentView(R.layout.activity_main);

        // A URL que você forneceu
        String urlAlvo = "https://www.sedepe.pe.gov.br/vaga-de-emprego/";

        // Avisa que a busca começou
        Toast.makeText(this, "Buscando vagas no site...", Toast.LENGTH_SHORT).show();

        // Chama o nosso motor de busca passando o site da SEDEPE
        WebScraper.findPdfLink(urlAlvo, new WebScraper.ScraperCallback() {
            @Override
            public void onLinkFound(String pdfUrl) {
                // O Android exige que mensagens na tela sejam chamadas na linha principal
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Imprime o link no console de desenvolvedor
                        Log.i("AppVagas", "PDF Encontrado: " + pdfUrl);

                        // Mostra o link na tela do celular
                        Toast.makeText(MainActivity.this, "Sucesso! Link capturado", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("AppVagas", "Erro: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Aviso: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}