package br.com.ucsal.server.remote;

import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.*;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import br.com.ucsal.common.remote.ClienteCallback;
import br.com.ucsal.server.domain.Partida;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BatalhaNavalServerImpl extends UnicastRemoteObject implements BatalhaNavalServer {
    private volatile Partida partida = new Partida();
    private final Map<Integer, ClienteCallback> callbacks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService batimentos = Executors.newSingleThreadScheduledExecutor();

    public BatalhaNavalServerImpl() throws RemoteException {
        super();
        batimentos.scheduleAtFixedRate(this::verificarJogadores, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public String retornarString() throws RemoteException {
        System.out.println("Chamada Recebida");
        return "Ola mundo";
    }

    @Override
    public synchronized int entrarNaPartida(String nome, ClienteCallback clienteCallback) throws
            RemoteException, PartidaIndisponivelException {
        if (partida.isEncerrada()) {
            partida = new Partida();
            callbacks.clear();
        }
        int id = partida.entrarNaPartida(nome);
        callbacks.put(id, clienteCallback);
        return id;
    }

    @Override
    public void posicionarNavio(int id, TipoNavio tipoNavio, Posicao inicial, Direcao direcao) throws
            RemoteException, JogadaInvalidaException, PartidaIndisponivelException {

        boolean comecou = partida.posicionarNavio(id, tipoNavio, inicial, direcao);

        if (comecou) {
            callbacks.get(0).partidaIniciada(partida.getNome(1));
            callbacks.get(1).partidaIniciada(partida.getNome(0));
            callbacks.get(partida.getDeQuemVez()).suaVez();
        }
    }

    @Override
    public ResultadoTiro fazerJogada(int id, Posicao tiro) throws
            RemoteException, JogadaInvalidaException, NaoESuaVezException, PartidaIndisponivelException {
        ResultadoTiro resultado = partida.atirar(id, tiro);
        int adversario = 1 - id;

        callbacks.get(adversario).tiroRecebido(tiro, resultado);

        if (partida.isEncerrada()) {
            callbacks.get(0).fimDeJogo(partida.getNome(id), MotivoFim.AFUNDOU_EMBARCACOES);
            callbacks.get(1).fimDeJogo(partida.getNome(id), MotivoFim.AFUNDOU_EMBARCACOES);
        } else {
            callbacks.get(adversario).suaVez();
        }

        return resultado;
    }

    @Override
    public void sairDaPartida(int id, ClienteCallback clienteCallback) throws RemoteException {
        tratarDesconexao(id, clienteCallback);
    }

    private synchronized void tratarDesconexao(int idQueSaiu, ClienteCallback quemSaiu) {
        if (!quemSaiu.equals(callbacks.get(idQueSaiu)) || partida.isEncerrada()) {
            return;
        }

        if (partida.getQuantidadeJogadores() < 2) {
            partida = new Partida();
            callbacks.clear();
            return;
        }

        partida.encerrarPorDesconexao();
        callbacks.remove(idQueSaiu);

        int outro = 1 - idQueSaiu;
        ClienteCallback adversario = callbacks.get(outro);
        if (adversario != null) {
            try {
                adversario.fimDeJogo(partida.getNome(outro), MotivoFim.JOGADOR_DESCONECTADO);
            } catch (RemoteException e) {
            }
        }
    }

    private void verificarJogadores() {
        try {
            for (Map.Entry<Integer, ClienteCallback> entrada : callbacks.entrySet()) {
                try {
                    entrada.getValue().ping();
                } catch (RemoteException e) {
                    tratarDesconexao(entrada.getKey(), entrada.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}