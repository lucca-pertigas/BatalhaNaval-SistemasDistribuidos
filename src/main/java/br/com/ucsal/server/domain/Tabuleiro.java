package br.com.ucsal.server.domain;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.model.Direcao;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tabuleiro {
    private static final int TAMANHO_TABULEIRO = 10;

    private final TipoNavio[][] gradeNavios;
    private final boolean[][] gradeTiros;
    private final Map<TipoNavio, Integer> casasRestantes;

    public Tabuleiro() {
        gradeNavios = new TipoNavio[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
        gradeTiros = new boolean[TAMANHO_TABULEIRO][TAMANHO_TABULEIRO];
        casasRestantes = new HashMap<>();
    }

    public void posicionarNavio(TipoNavio tipoNavio, Posicao posicaoInicial, Direcao direcaoInicial) throws
            JogadaInvalidaException {

        if (casasRestantes.containsKey(tipoNavio)) {
            throw new JogadaInvalidaException("A embarcacao " + tipoNavio + " ja foi posicionado !");
        }

        List<Posicao> casas = new ArrayList<>();

        for (int i = 0; i < tipoNavio.getTamanho(); i++) {
            int linha;
            int coluna;

            if (direcaoInicial == Direcao.HORIZONTAL) {
                linha = posicaoInicial.getLinha();
                coluna = posicaoInicial.getColuna() + i;
            } else {
                linha = posicaoInicial.getLinha() + i;
                coluna = posicaoInicial.getColuna();
            }

            if(!estaDentroDoTabuleiro(linha, coluna)) {
                throw new JogadaInvalidaException("Valor for do tabuleiro");
            }

            if (gradeNavios[linha][coluna] != null) {
                throw new JogadaInvalidaException("Valor ja escolhido anteriormente");
            }

            casas.add(new Posicao(linha, coluna));
        }

        for (Posicao casa : casas) {
            gradeNavios[casa.getLinha()][casa.getColuna()] = tipoNavio;
        }

        casasRestantes.put(tipoNavio, tipoNavio.getTamanho());

    }

    private boolean estaDentroDoTabuleiro(int linha, int coluna) {
        if (linha >= 0 && linha < TAMANHO_TABULEIRO && coluna >= 0 && coluna < TAMANHO_TABULEIRO) {
            return true;
        } else {
            return false;
        }
    }

    public ResultadoTiro receberTiro(Posicao posicao) throws JogadaInvalidaException {
        int linha = posicao.getLinha();
        int coluna = posicao.getColuna();

        if (!estaDentroDoTabuleiro(linha, coluna) ) {
            throw new JogadaInvalidaException("Tiro fora do tabuleiro");
        }

        if (gradeTiros[linha][coluna] == true) {
            throw new JogadaInvalidaException("Essa casa já foi atacada");

        }
        gradeTiros[linha][coluna] = true;

        TipoNavio navio = gradeNavios[linha][coluna];

        if (navio == null) {
            return ResultadoTiro.AGUA;
        }

        int restantes = casasRestantes.get(navio) - 1;
        casasRestantes.put(navio, restantes);

        if (restantes == 0) {
            return ResultadoTiro.AFUNDOU;
        } else {
            return ResultadoTiro.ACERTOU;
        }

    }

    public boolean frotaCompleta() {
        if (casasRestantes.size() == TipoNavio.values().length) {
            return true;
        } else {
            return false;
        }
    }

    public boolean frotaAfundada() {
        for (int barcosRestantes : casasRestantes.values()) {
            if (barcosRestantes > 0) {
                return false;
            }
        }

        return true;
    }
}
