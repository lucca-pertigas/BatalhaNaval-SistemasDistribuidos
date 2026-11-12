package br.com.ucsal.client;

import br.com.ucsal.client.remote.ClienteCallbackImpl;
import br.com.ucsal.common.exception.JogadaInvalidaException;
import br.com.ucsal.common.exception.NaoESuaVezException;
import br.com.ucsal.common.exception.PartidaIndisponivelException;
import br.com.ucsal.common.model.*;
import br.com.ucsal.common.remote.BatalhaNavalServer;
import br.com.ucsal.common.remote.ClienteCallback;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class IniciarCliente {
    public static void main(String[] args) throws Exception{

        Registry registro = LocateRegistry.getRegistry("localhost", 1099);
        BatalhaNavalServer servidor = (BatalhaNavalServer) registro.lookup("BatalhaNaval");

        Scanner sc = new Scanner(System.in);

        System.out.println("Digite seu nome: ");
        String nome = sc.nextLine();

        ClienteCallback callback = new ClienteCallbackImpl(new OuvinteDoJogo() {
            @Override
            public void partidaIniciada(String nomeAdversario) {
                System.out.println("A partida começou! Seu adversário é " + nomeAdversario);
            }

            @Override
            public void suaVez() {
                System.out.println("É a sua vez! Digite linha e coluna:");
            }

            @Override
            public void tiroRecebido(Posicao posicao, ResultadoTiro resultadoTiro) {
                System.out.println("O adversário atirou em  + posicao + :  + resultadoTiro");
            }

            @Override
            public void fimDeJogo(String nomeVencedor, MotivoFim motivoFim) {
                System.out.println("Fim de jogo! Vencedor: " + nomeVencedor + " (" + motivoFim + ")");
            }
        });

        int id = servidor.entrarNaPartida(nome, callback);
        System.out.println("Você entrou como jogador " + id);

        int linhaNavio = 0;
        for (TipoNavio tipo : TipoNavio.values()) {
            servidor.posicionarNavio(id, tipo, new Posicao(linhaNavio, 0), Direcao.HORIZONTAL);
            linhaNavio++;
        }

        System.out.println("Frota posicionada, Aguardando adversário...");

        while (true) {
            int linha = sc.nextInt();
            int coluna = sc.nextInt();

            try {
                ResultadoTiro resultado = servidor.fazerJogada(id, new Posicao(linha, coluna));
                System.out.println("Resultado do seu tiro: " + resultado);
            } catch (NaoESuaVezException | JogadaInvalidaException | PartidaIndisponivelException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }
}
