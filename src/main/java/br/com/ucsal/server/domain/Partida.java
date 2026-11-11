package br.com.ucsal.server.domain;

import br.com.ucsal.common.exception.PartidaIndisponivelException;

import java.util.ArrayList;
import java.util.List;

public class Partida {

    private final List<String> nomes = new ArrayList<>();
    private final List<Tabuleiro> tabuleiros = new ArrayList<>();
    private FasePartida fase = FasePartida.AGUARDANDO_JOGADORES;
    private int deQuemVez;

    public synchronized int entrarNaPartida(String nome) throws PartidaIndisponivelException {

        if(fase != FasePartida.AGUARDANDO_JOGADORES) {
            throw new PartidaIndisponivelException("A partida ja esta cheia !");
        }

        int id = nomes.size();
        nomes.add(nome);
        tabuleiros.add(new Tabuleiro());

        if (nomes.size() == 2) {
            fase = FasePartida.POSICIONANDO;
        }
        return id;
    }
}
