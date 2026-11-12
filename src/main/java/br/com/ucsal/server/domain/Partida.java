package br.com.ucsal.server.domain;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.Direcao;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;
import br.com.ucsal.common.model.TipoNavio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public synchronized boolean posicionarNavio(int id, TipoNavio tipoNavio, Posicao inicial, Direcao direcao)
    throws JogadaInvalidaException, PartidaIndisponivelException {
        if (fase != FasePartida.POSICIONANDO) {
            throw new PartidaIndisponivelException("Não é permitido posicionar embarcação agora !");
        }

        tabuleiros.get(id).posicionarNavio(tipoNavio, inicial, direcao);

        if(tabuleiros.get(0).frotaCompleta() && tabuleiros.get(1).frotaCompleta()) {
            fase = FasePartida.EM_JOGO;
            deQuemVez = new Random().nextInt(2);
            return true;
        }

        return false;
    }

    public synchronized ResultadoTiro atirar(int id, Posicao alvo) throws NaoESuaVezException,
            JogadaInvalidaException, PartidaIndisponivelException {
        if (fase != FasePartida.EM_JOGO) {
            throw new PartidaIndisponivelException("A partida não está em andamento");
        }

        if(id != deQuemVez){
            throw new NaoESuaVezException("Não é sua vez de jogar");
        }

        int adversario = 1 - id;

        ResultadoTiro resultado = tabuleiros.get(adversario).receberTiro(alvo);

        if (tabuleiros.get(adversario).frotaAfundada()) {
            fase = FasePartida.ENCERRADA;
        } else {
            deQuemVez = adversario;
        }
        return resultado;
    }

    public synchronized String getNome(int id) {
        return nomes.get(id);
    }

    public synchronized int getDeQuemVez() {
        return deQuemVez;
    }

    public synchronized boolean isEncerrada() {
        return fase == FasePartida.ENCERRADA;
    }

    public synchronized boolean isEmJogo() {
        return fase == FasePartida.EM_JOGO;
    }
}
