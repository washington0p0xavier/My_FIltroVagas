package com.example.my_filtrovagas;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import androidx.appcompat.app.AppCompatActivity;

// Importe a classe que criamos. Ajuste o caminho se necessário.
import com.controller.WhatsAppSender;
import com.example.my_filtrovagas.R;
import com.controller.PdfProcessor;
import com.controller.WebScraper;



public class MainActivity extends AppCompatActivity {

    private LinearLayout layoutLoading;
    private static final String PREFS_NAME = "vagas_prefs";
    private static final String HISTORY_PREFIX = "vagas_history_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutLoading = findViewById(R.id.layoutLoading);

        // Configuração do botão de limpeza de histórico
        findViewById(R.id.btnLimparHistorico).setOnClickListener(v -> {
            limparHistorico();
        });

        // 1. Configura o clique do botão da Ala Garanhuns
        findViewById(R.id.btnRegiaoGaranhuns).setOnClickListener(v -> {
            String[] cidadesRegiaoGaranhuns = {
                "GARANHUNS", "SÃO JOÃO", "BREJÃO", "PARANATAMA", "CAETÉS",
                "JUCATI", "JUPI", "ANGELIM", "CAPOEIRAS", "SALOÁ",
                "TEREZINHA", "CALÇADO", "LAGOA DO OURO"
            };
            executarAutomacao("Região Garanhuns", cidadesRegiaoGaranhuns);
        });

        // 2. Configura o clique do botão da Ala Bom Conselho
        findViewById(R.id.btnRegiaoBomComselho).setOnClickListener(v -> {
            String[] cidadesRegiaoBomConselho = {
                "BOM CONSELHO", "TEREZINHA", "BREJÃO", "LAGOA DO OURO",
                "SALOÁ", "IATI", "PALMEIRA DOS ÍNDIOS"
            };
            executarAutomacao("Região Bom Conselho", cidadesRegiaoBomConselho);
        });

        // 3. Configura o clique do botão do Ala Arcoverde
        findViewById(R.id.btnRegiaoArcoverde).setOnClickListener(v -> {
            String[] cidadesRegiaoArcoverde = {
                "ARCOVERDE", "PEDRA", "BUÍQUE", "VENTUROSA", "ALAGOINHA", "SERTÂNIA"
            };
            executarAutomacao("Região Arcoverde", cidadesRegiaoArcoverde);
        });

        // 4. Configura o clique do botão do Ramo Pesqueira
        findViewById(R.id.btnRegiaoPesqueira).setOnClickListener(v -> {
            String[] cidadesRegiaoPesqueira = {
                "PESQUEIRA", "SANHARÓ", "ALAGOINHA", "POÇÃO", "BELO JARDIM"
            };
            executarAutomacao("Região Pesqueira", cidadesRegiaoPesqueira);
        });

        // 5. Configura o clique do botão do Ramo Águas Belas
        findViewById(R.id.btnRegiaoAguaBelas).setOnClickListener(v -> {
            String[] cidadesRegiaoAguaBelas = {
                "ÁGUAS BELAS", "IATI", "ITAÍBA", "SANTANA DO IPANEMA", "OURO BRANCO"
            };
            executarAutomacao("Região Águas Belas", cidadesRegiaoAguaBelas);
        });

        // 6. Configura o clique do botão da Região Buíque
        findViewById(R.id.btnRegiaoBuique).setOnClickListener(v -> {
            String[] cidadesRegiaoBuique = {
                "BUÍQUE", "TUPANATINGA", "PEDRA", "ARCOVERDE", "VENTUROSA"
            };
            executarAutomacao("Região Buíque", cidadesRegiaoBuique);
        });

        // Caso queira adicionar mais botões das outras regiões, basta seguir o mesmo padrão aqui embaixo
    }

    /**
     * Motor dinâmico que realiza a busca, extração e filtragem com base na região selecionada.
     */
    private void executarAutomacao(final String nomeRegiao, final String[] cidadesAlvo) {
        String urlAlvo = "https://www.sedepe.pe.gov.br/vaga-de-emprego/";
        
        // Mostra o carregamento na tela
        if (layoutLoading != null) layoutLoading.setVisibility(View.VISIBLE);
        
        // Passos em segundo plano para não travar a interface do usuário
        WebScraper.findPdfLink(urlAlvo, new WebScraper.ScraperCallback() {
            @Override
            public void onLinkFound(final String pdfUrl) {

                // Conectando ao processador de PDF
                PdfProcessor.extractTextFromUrl(MainActivity.this, pdfUrl, new PdfProcessor.PdfCallback() {
                    @Override
                    public void onTextExtracted(final String rawText) {

                        // Retorna para a Thread principal para interagir com o app e o WhatsApp
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder vagasParaWhatsApp = new StringBuilder();
                                vagasParaWhatsApp.append("📌 *VAGAS DA AGÊNCIA DO TRABALHO — ").append(nomeRegiao.toUpperCase()).append("*\n\n");

                                // Recupera o histórico de vagas já enviadas para esta região
                                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                String historyKey = HISTORY_PREFIX + nomeRegiao.replace(" ", "_");
                                Set<String> historicoSet = prefs.getStringSet(historyKey, new HashSet<>());
                                
                                // Criamos um novo set para atualizar o histórico depois
                                Set<String> novoHistoricoSet = new HashSet<>(historicoSet);
                                
                                // Mapa para agrupar as novas vagas por cidade
                                Map<String, StringBuilder> agrupamentoNovasVagas = new LinkedHashMap<>();

                                // Divide o texto do PDF linha por linha
                                String[] linhas = rawText.split("\n");
                                boolean encontrouAlgumaVagaNoSite = false;
                                boolean temNovaVagaParaEnviar = false;

                                // Varre as linhas procurando as cidades dessa região específica
                                for (String linha : linhas) {
                                    String linhaLimpa = linha.trim();
                                    if (linhaLimpa.isEmpty()) continue;

                                    String linhaMaiuscula = linhaLimpa.toUpperCase();

                                    for (String cidade : cidadesAlvo) {
                                        String cidadeMaiuscula = cidade.toUpperCase();
                                        if (linhaMaiuscula.contains(cidadeMaiuscula)) {
                                            
                                            String vagaFormatada = formatarLinhaVaga(linhaLimpa, cidadeMaiuscula);

                                            // Só processa se a linha realmente contiver uma vaga
                                            if (!vagaFormatada.isEmpty()) {
                                                encontrouAlgumaVagaNoSite = true;
                                                
                                                // Identificador único da vaga é a própria linha formatada
                                                String idVaga = vagaFormatada.trim();

                                                // SEMPRE adicionamos ao novo histórico para manter sincronizado com o site
                                                novoHistoricoSet.add(idVaga);

                                                // Só adicionamos na mensagem do WhatsApp se NÃO estiver no histórico antigo
                                                if (!historicoSet.contains(idVaga)) {
                                                    if (!agrupamentoNovasVagas.containsKey(cidadeMaiuscula)) {
                                                        agrupamentoNovasVagas.put(cidadeMaiuscula, new StringBuilder());
                                                    }
                                                    agrupamentoNovasVagas.get(cidadeMaiuscula).append(vagaFormatada).append("\n");
                                                    temNovaVagaParaEnviar = true;
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }

                                // Salva o histórico atualizado (vagas que existem no site agora)
                                prefs.edit().putStringSet(historyKey, novoHistoricoSet).apply();

                                // Monta a mensagem final com as novidades
                                if (temNovaVagaParaEnviar) {
                                    for (Map.Entry<String, StringBuilder> entry : agrupamentoNovasVagas.entrySet()) {
                                        vagasParaWhatsApp.append("💼 *VAGAS EM ").append(entry.getKey()).append("*\n\n");
                                        vagasParaWhatsApp.append(entry.getValue().toString()).append("\n");
                                    }
                                    
                                    Toast.makeText(MainActivity.this, "Novas vagas encontradas! Abrindo WhatsApp...", Toast.LENGTH_SHORT).show();
                                    WhatsAppSender.enviarMensagem(MainActivity.this, vagasParaWhatsApp.toString());
                                } else {
                                    if (!encontrouAlgumaVagaNoSite) {
                                        Toast.makeText(MainActivity.this, "Nenhuma vaga disponível no site para " + nomeRegiao, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Nenhuma vaga NOVA para " + nomeRegiao + ". (Você já viu todas)", Toast.LENGTH_LONG).show();
                                    }
                                }

                                // Esconde o carregamento
                                if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        runOnUiThread(() -> {
                            Log.e("AppVagas", "Erro no PDF para " + nomeRegiao + ": " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Erro ao processar o arquivo PDF.", Toast.LENGTH_SHORT).show();
                            if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                        });
                    }
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(() -> {
                    Log.e("AppVagas", "Erro no site para " + nomeRegiao + ": " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Erro de conexão com o site da SEDEPE.", Toast.LENGTH_SHORT).show();
                    if (layoutLoading != null) layoutLoading.setVisibility(View.GONE);
                });
            }
        });
    }

    /**
     * Formata uma linha bruta do PDF para o padrão:
     * ➡️ Nº VAGAS: X - CARGO - CIDADE - CONTRATO - SALÁRIO - ESCOLARIDADE - EXPERIÊNCIA
     */
    private String formatarLinhaVaga(String linha, String cidade) {
        // 1. Limpeza inicial de múltiplos espaços para um separador padrão
        // Aqui usamos a lógica de que 2 ou mais espaços representam uma troca de coluna no PDF
        String tratada = linha.replaceAll("\\s{2,}", " - ");

        // 2. Tenta extrair o número de vagas que costuma vir no início da linha (ex: "1 ENGENHEIRO")
        String numVagas = "1"; // Valor padrão caso não encontre
        String cargoRestante = tratada;

        // Expressão regular para pegar o primeiro número no início da linha
        if (tratada.matches("^\\d+.*")) {
            String[] partes = tratada.split(" ", 2);
            if (partes.length > 1) {
                numVagas = partes[0];
                cargoRestante = partes[1];
            }
        }

        // 3. Monta a string final com emojis e o formato solicitado
        StringBuilder sb = new StringBuilder();

        // Transforma o restante em maiúsculo e garante que a cidade tenha os separadores corretos
        String resultado = cargoRestante.toUpperCase();

        // Se a cidade estiver "grudada", garante o separador
        if (resultado.contains(cidade) && !resultado.contains(" - " + cidade + " - ")) {
            resultado = resultado.replace(cidade, " - " + cidade + " - ");
        }

        // Limpezas de traços e espaços
        resultado = resultado.replace(" -  - ", " - ");
        resultado = resultado.replace(" - - ", " - ");
        resultado = resultado.replaceAll("^- ", ""); // Remove traço no início
        resultado = resultado.trim();

        // VALIDAÇÃO RIGOROSA: Remove todos os hífens e espaços para ver se sobra algo além da cidade
        String apenasConteudo = resultado.replace("-", "").replace(" ", "").trim();
        String cidadeSemEspaco = cidade.replace(" ", "").trim();

        if (apenasConteudo.equals(cidadeSemEspaco) || apenasConteudo.isEmpty()) {
            return "";
        }

        sb.append("➡️ Nº VAGAS: ").append(numVagas).append(" - ");
        sb.append(resultado);

        // 4. Adiciona "EXPERIÊNCIA:" e evita a repetição do número
        String finalStr = sb.toString().trim().replaceAll(" - $", "");

        if (finalStr.matches(".*\\d+ MESES$") && !finalStr.contains("EXPERIÊNCIA")) {
             String ultimoNum = extractLastNumber(finalStr);
             // Remove o número solto que ficaria antes de "EXPERIÊNCIA"
             if (!ultimoNum.isEmpty()) {
                 int lastIndex = finalStr.lastIndexOf(ultimoNum);
                 if (lastIndex > 0) {
                     finalStr = finalStr.substring(0, lastIndex).trim();
                     // Remove traço residual se houver
                     if (finalStr.endsWith("-")) {
                         finalStr = finalStr.substring(0, finalStr.length() - 1).trim();
                     }
                 }
             }
             finalStr = finalStr + " - EXPERIÊNCIA: " + ultimoNum + " MESES";
        }

        return finalStr;
    }

    private String extractLastNumber(String text) {
        String[] words = text.split(" ");
        for (int i = words.length - 1; i >= 0; i--) {
            if (words[i].matches("\\d+")) return words[i];
        }
        return "";
    }

    private void limparHistorico() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Memória limpa! Todas as vagas serão enviadas na próxima busca.", Toast.LENGTH_LONG).show();
    }
}
