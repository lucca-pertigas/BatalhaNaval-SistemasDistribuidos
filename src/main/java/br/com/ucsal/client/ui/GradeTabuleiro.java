package br.com.ucsal.client.ui;

import br.com.ucsal.common.model.Posicao;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Componente visual de um tabuleiro 10x10, reaproveitado nas telas
 * de posicionamento e de batalha.
 *
 * Ele NÃO sabe nada de regras do jogo: só desenha células e avisa
 * a tela quando o mouse entra, sai ou clica numa célula.
 * Quem decide o que cada clique significa é a tela.
 *
 *        1   2   3  ...  10
 *    A  [ ] [ ] [ ]     [ ]
 *    B  [ ] [ ] [ ]     [ ]
 *    ...
 */
public class GradeTabuleiro {

    private static final int N = BatalhaNavalApp.TAMANHO_TABULEIRO;
    private static final double TAMANHO_CELULA = 34;

    private final GridPane grade = new GridPane();
    private final StackPane[][] celulas = new StackPane[N][N];
    private final Label[][] simbolos = new Label[N][N];

    // Estado "fixo" de cada célula (ex.: "navio", "agua", "acerto") -> classe CSS
    private final String[][] estados = new String[N][N];
    // Células destacadas temporariamente (prévia ao passar o mouse)
    private final List<Posicao> previa = new ArrayList<>();
    private String estiloPrevia;

    // O que fazer em cada evento. Começam "vazios" para nunca serem null.
    private Consumer<Posicao> aoClicar = posicao -> { };
    private Consumer<Posicao> aoEntrar = posicao -> { };
    private Runnable aoSair = () -> { };
    private Runnable aoClicarDireito = () -> { };

    public GradeTabuleiro() {
        grade.getStyleClass().add("grade");
        grade.setHgap(3);
        grade.setVgap(3);

        // Cabeçalho das colunas: 1..10 (linha 0 do GridPane)
        for (int coluna = 0; coluna < N; coluna++) {
            Label numero = new Label(String.valueOf(coluna + 1));
            numero.getStyleClass().add("coordenada");
            GridPane.setHalignment(numero, HPos.CENTER);
            grade.add(numero, coluna + 1, 0);
        }

        for (int linha = 0; linha < N; linha++) {
            // Cabeçalho das linhas: A..J (coluna 0 do GridPane)
            Label letra = new Label(Textos.letraDaLinha(linha));
            letra.getStyleClass().add("coordenada");
            grade.add(letra, 0, linha + 1);

            for (int coluna = 0; coluna < N; coluna++) {
                criarCelula(linha, coluna);
            }
        }
    }

    private void criarCelula(int linha, int coluna) {
        Label simbolo = new Label();
        simbolo.getStyleClass().add("simbolo");

        StackPane celula = new StackPane(simbolo);
        celula.getStyleClass().add("celula");
        celula.setPrefSize(TAMANHO_CELULA, TAMANHO_CELULA);
        celula.setMinSize(TAMANHO_CELULA, TAMANHO_CELULA);

        // Cada célula "lembra" a própria posição (variável capturada pela lambda)
        Posicao posicao = new Posicao(linha, coluna);

        celula.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.SECONDARY) {
                aoClicarDireito.run();
            } else {
                aoClicar.accept(posicao);
            }
        });
        celula.setOnMouseEntered(evento -> aoEntrar.accept(posicao));
        celula.setOnMouseExited(evento -> aoSair.run());

        celulas[linha][coluna] = celula;
        simbolos[linha][coluna] = simbolo;
        grade.add(celula, coluna + 1, linha + 1); // +1 por causa dos cabeçalhos
    }

    /** O nó JavaFX para colocar dentro de um layout. */
    public GridPane getNo() {
        return grade;
    }

    // ------------------------------------------------------------------
    // Quem usa a grade registra o que fazer em cada evento
    // ------------------------------------------------------------------

    public void setAoClicar(Consumer<Posicao> acao) { this.aoClicar = acao; }
    public void setAoEntrar(Consumer<Posicao> acao) { this.aoEntrar = acao; }
    public void setAoSair(Runnable acao) { this.aoSair = acao; }
    public void setAoClicarDireito(Runnable acao) { this.aoClicarDireito = acao; }

    /** Liga/desliga o visual de "pode clicar aqui" (cursor de mão + hover). */
    public void setClicavel(boolean clicavel) {
        grade.getStyleClass().remove("grade-clicavel");
        if (clicavel) {
            grade.getStyleClass().add("grade-clicavel");
        }
    }

    // ------------------------------------------------------------------
    // Pintura das células
    // ------------------------------------------------------------------

    /**
     * Define o estado fixo de uma célula.
     * @param estilo classe CSS ("navio", "agua", "acerto", "afundado") ou null
     * @param simbolo texto dentro da célula ("•", "✕") ou "" para nenhum
     */
    public void marcar(Posicao posicao, String estilo, String simbolo) {
        int l = posicao.getLinha();
        int c = posicao.getColuna();
        estados[l][c] = estilo;
        simbolos[l][c].setText(simbolo);
        atualizarEstilo(l, c);
    }

    public String getEstado(Posicao posicao) {
        return estados[posicao.getLinha()][posicao.getColuna()];
    }

    /** Destaca temporariamente algumas células (ex.: onde o navio ficaria). */
    public void mostrarPrevia(List<Posicao> casas, boolean valida) {
        limparPrevia();
        estiloPrevia = valida ? "previa-ok" : "previa-erro";
        for (Posicao p : casas) {
            if (dentro(p)) { // a prévia pode "vazar" para fora do tabuleiro
                previa.add(p);
                atualizarEstilo(p.getLinha(), p.getColuna());
            }
        }
    }

    public void limparPrevia() {
        List<Posicao> antigas = new ArrayList<>(previa);
        previa.clear();
        for (Posicao p : antigas) {
            atualizarEstilo(p.getLinha(), p.getColuna());
        }
    }

    /** Recalcula as classes CSS de uma célula: base + estado + prévia. */
    private void atualizarEstilo(int linha, int coluna) {
        List<String> classes = new ArrayList<>();
        classes.add("celula");
        if (estados[linha][coluna] != null) {
            classes.add(estados[linha][coluna]);
        }
        if (previa.contains(new Posicao(linha, coluna))) {
            classes.add(estiloPrevia);
        }
        celulas[linha][coluna].getStyleClass().setAll(classes);
    }

    public static boolean dentro(Posicao p) {
        return p.getLinha() >= 0 && p.getLinha() < N
                && p.getColuna() >= 0 && p.getColuna() < N;
    }
}