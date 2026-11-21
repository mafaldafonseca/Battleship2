package battleship;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfExporter {

    // Lista em memória para acumular as jogadas e informações durante a partida
    private List<String> historicoPartida;

    public PdfExporter() {
        this.historicoPartida = new ArrayList<>();
    }

    // Exemplo de como capturar o output de qualquer função void (como o método de mostrar tiros ou o mapa)
    public String capturarOutputDoJogo(Runnable funcaoDoJogo) {
        // Guarda o PrintStream original da consola
        PrintStream oldOut = System.out;

        // Cria um fluxo de memória para reencaminhar o print
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream novoPrint = new PrintStream(baos);

        try {
            // Redireciona o System.out para a nossa memória
            System.setOut(novoPrint);

            // Executa a função void original do jogo (ex: () -> MeuJogo.imprimirTiros())
            funcaoDoJogo.run();

        } finally {
            // Restaura o System.out original obrigatoriamente para não estragar a consola
            System.setOut(oldOut);
        }

        // Devolve todo o texto impresso como uma única String
        return baos.toString();
    }

    /**
     * Adiciona uma linha de texto (uma jogada, um tiro, status, etc.) à lista em memória.
     * @param linha Texto a registar.
     */
    public void escrever(String linha) {
        if (linha != null && !linha.isEmpty()) {
            historicoPartida.add(linha);
        }
    }

    public List<String> historicoAtual() {
        return historicoPartida;
    }

    /**
     * Lê/retorna a lista completa de linhas guardadas até ao momento.
     * @return Lista de strings com o histórico acumulado.
     */
    public List<String> lerHistorico() {
        return new ArrayList<>(historicoPartida);
    }

    /**
     * Limpa o histórico guardado.
     */
    public void limpar() {
        historicoPartida.clear();
    }

    /**
     * Exporta todo o conteúdo acumulado em memória para um ficheiro PDF no final da partida.
     * @param nomeFicheiro Caminho/nome do ficheiro PDF a gerar (ex: "relatorio_battleship.pdf").
     */
    public void exportarParaPdf(String nomeFicheiro) {
        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
            contentStream.newLineAtOffset(50, 750);

            contentStream.showText("--- Battleship2 ---");
            contentStream.newLineAtOffset(0, -30);

            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

            int blocosNaPagina = 0;

            for (String bloco : historicoPartida) {
                if (bloco == null) continue;

                // Se já atingimos 3 blocos nesta página, mudamos de página
                if (blocosNaPagina >= 3) {
                    contentStream.endText();
                    contentStream.close(); // Fecha o stream da página atual

                    // Cria uma nova página
                    page = new PDPage();
                    document.addPage(page);

                    contentStream = new PDPageContentStream(document, page);
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    contentStream.newLineAtOffset(50, 750); // Recomeça no topo da nova página

                    blocosNaPagina = 0;
                }

                // Escreve um identificador ou título para o bloco (opcional)
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

                // Divide o conteúdo interno do bloco por linhas caso tenha quebras de linha (\n)
                String[] linhas = bloco.replace("\r", "").split("\n");
                for (String linha : linhas) {
                    if (!linha.trim().isEmpty()) {
                        contentStream.showText(linha);
                        contentStream.newLineAtOffset(0, -15);
                    }
                }

                // Espaço extra entre blocos diferentes
                contentStream.newLineAtOffset(0, -25);

                // Incrementa o contador de blocos na página atual
                blocosNaPagina++;
            }

            contentStream.endText();
            contentStream.close();

            document.save(nomeFicheiro);
            System.out.println("PDF gerado com sucesso: " + nomeFicheiro);

        } catch (IOException e) {
            System.err.println("Erro ao gerar o PDF da partida: " + e.getMessage());
            e.printStackTrace();
        }
    }
}