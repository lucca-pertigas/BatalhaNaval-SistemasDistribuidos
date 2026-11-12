package br.com.ucsal.client;

import br.com.ucsal.common.model.MotivoFim;
import br.com.ucsal.common.model.Posicao;
import br.com.ucsal.common.model.ResultadoTiro;

public interface OuvinteDoJogo {

    public void partidaIniciada(String nomeAdversario);

    public void suaVez();

    public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro);

    public void fimDeJogo (String nomeVencedor, MotivoFim motivoFim);
}
