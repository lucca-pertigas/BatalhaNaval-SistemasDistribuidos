package br.com.ucsal.comum;

import java.io.Serializable;
import java.util.Objects;

public class Posicao implements Serializable{
    private final int linha;
    private final int coluna;

    public Posicao(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    public int getColuna() {
        return coluna;
    }

    public int getLinha() {
        return linha;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Posicao posicao = (Posicao) o;
        return linha == posicao.linha && coluna == posicao.coluna;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linha, coluna);
    }

    @Override
    public String toString() {
        return "Posicao{" +
                "linha=" + linha +
                ", coluna=" + coluna +
                '}';
    }
}
