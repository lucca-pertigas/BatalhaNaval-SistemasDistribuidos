package br.com.ucsal.client.ui;

import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.TipoNavio;

/**
 * Textos "bonitos" para mostrar na tela.
 * Os enums e a Posicao do backend continuam iguais: aqui só
 * traduzimos para algo que o jogador entende.
 */
public final class Textos {

    private static final String LETRAS = "ABCDEFGHIJ";

    private Textos() { } // classe utilitária: não se cria objeto dela

    /** PORTA_AVIOES -> "Porta-aviões" */
    public static String nomeNavio(TipoNavio tipo) {
        return switch (tipo) {
            case PORTA_AVIOES -> "Porta-aviões";
            case ENCOURACADO -> "Encouraçado";
            case CRUZADOR -> "Cruzador";
            case SUBMARINO -> "Submarino";
            case DESTROIER -> "Destróier";
        };
    }

    /** Letra da linha: 0 -> "A", 1 -> "B"... */
    public static String letraDaLinha(int linha) {
        return String.valueOf(LETRAS.charAt(linha));
    }

    /** Posicao(linha=0, coluna=4) -> "A5" (coluna começa em 1 para o jogador) */
    public static String nomeDaCasa(Posicao posicao) {
        return letraDaLinha(posicao.getLinha()) + (posicao.getColuna() + 1);
    }
}