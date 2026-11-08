package br.com.ucsal.common.model;

public enum TipoNavio {
    PORTA_AVIOES(5),
    ENCOURACADO(4),
    CRUZADOR(3),
    SUBMARINO(3),
    DESTROIER(2);

    private final int tamanho;

    TipoNavio(int tamanho) {
        this.tamanho = tamanho;
    }

    public int getTamanho() {
        return tamanho;
    }
}
