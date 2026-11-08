package br.com.ucsal.server.domain;

import br.com.ucsal.common.model.TipoNavio;

import java.util.HashMap;
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
}
