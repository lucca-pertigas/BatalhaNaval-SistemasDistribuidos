package br.com.ucsal.client;

import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;

/**
 * "Ouvinte" dos avisos que chegam do servidor.
 *
 * Cada tela implementa só os métodos que interessam a ela.
 * Os "default" vazios permitem não implementar o resto.
 *
 * Todos estes métodos são chamados JÁ na thread do JavaFX
 * (o ClienteCallbackImpl cuida disso), então podem mexer na tela.
 */
public interface EventosDoJogo {

    default void partidaIniciada(String nomeAdversario) { }

    default void suaVez() { }

    default void tiroRecebido(Posicao posicao, ResultadoTiro resultado) { }

    default void fimDeJogo(String nomeVencedor, MotivoFim motivo) { }
}