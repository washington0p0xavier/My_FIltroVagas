package com.example.my_filtrovagas;


import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.Map;
import androidx.appcompat.app.AppCompatActivity;

// Importe a classe que criamos. Ajuste o caminho se necessário.
import com.controller.WhatsAppSender;
import com.example.my_filtrovagas.R;
import com.controller.PdfProcessor;
import com.controller.WebScraper;



public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Caso queira adicionar mais botões das outras regiões, basta seguir o mesmo padrão aqui embaixo
    }

    /**
     * Motor dinâmico que realiza a busca, extração e filtragem com base na região selecionada.
     */
    private void executarAutomacao(final String nomeRegiao, final String[] cidadesAlvo) {
        String urlAlvo = "https://www.sedepe.pe.gov.br/vaga-de-emprego/";
        Toast.makeText(this, "Iniciando busca: " + nomeRegiao, Toast.LENGTH_SHORT).show();

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
                                StringBuilder vagasFiltradas = new StringBuilder();
                                vagasFiltradas.append("📌 *VAGAS DA AGÊNCIA DO TRABALHO — ").append(nomeRegiao.toUpperCase()).append("*\n\n");

                                // Mapa para agrupar as vagas por cidade
                                Map<String, StringBuilder> agrupamentoPorCidade = new LinkedHashMap<>();

                                // Divide o texto do PDF linha por linha
                                String[] linhas = rawText.split("\n");
                                boolean encontrouVaga = false;

                                // Varre as linhas procurando as cidades dessa região específica
                                for (String linha : linhas) {
                                    String linhaLimpa = linha.trim();
                                    if (linhaLimpa.isEmpty()) continue;

                                    String linhaMaiuscula = linhaLimpa.toUpperCase();

                                    for (String cidade : cidadesAlvo) {
                                        String cidadeMaiuscula = cidade.toUpperCase();
                                        if (linhaMaiuscula.contains(cidadeMaiuscula)) {
                                            
                                            if (!agrupamentoPorCidade.containsKey(cidadeMaiuscula)) {
                                                agrupamentoPorCidade.put(cidadeMaiuscula, new StringBuilder());
                                            }
                                            
                                            String vagaFormatada = formatarLinhaVaga(linhaLimpa, cidadeMaiuscula);
                                            
                                            // Só adiciona se a linha realmente contiver uma vaga (não for uma linha fantasma)
                                            if (!vagaFormatada.isEmpty()) {
                                                agrupamentoPorCidade.get(cidadeMaiuscula).append(vagaFormatada).append("\n");
                                                encontrouVaga = true;
                                            }
                                            break; 
                                        }
                                    }
                                }

                                // Monta a mensagem final agrupada
                                if (encontrouVaga) {
                                    for (Map.Entry<String, StringBuilder> entry : agrupamentoPorCidade.entrySet()) {
                                        vagasFiltradas.append("💼 *VAGAS EM ").append(entry.getKey()).append("*\n\n");
                                        vagasFiltradas.append(entry.getValue().toString()).append("\n");
                                    }
                                }

                                // Verifica se encontramos resultados para gerar o disparo
                                if (!encontrouVaga) {
                                    Toast.makeText(MainActivity.this, "Nenhuma vaga encontrada para " + nomeRegiao, Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Vagas filtradas! Abrindo WhatsApp...", Toast.LENGTH_SHORT).show();
                                    WhatsAppSender.enviarMensagem(MainActivity.this, vagasFiltradas.toString());
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Exception e) {
                        runOnUiThread(() -> {
                            Log.e("AppVagas", "Erro no PDF para " + nomeRegiao + ": " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Erro ao processar o arquivo PDF.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(() -> {
                    Log.e("AppVagas", "Erro no site para " + nomeRegiao + ": " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Erro de conexão com o site da SEDEPE.", Toast.LENGTH_SHORT).show();
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
}
