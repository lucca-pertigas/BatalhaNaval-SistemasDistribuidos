package br.com.ucsal.client.ui;

/**
 * Ponto de entrada do cliente (é ESTE que você roda pelo Run).
 *
 * Por que não rodar o BatalhaNavalApp direto?
 * Quando a classe com main() estende Application e o projeto não tem
 * module-info.java, o Java recusa com "JavaFX runtime components are missing".
 * Uma classe comum que só repassa a chamada contorna isso.
 */
public class ClienteLauncher {
    public static void main(String[] args) {
        BatalhaNavalApp.main(args);
    }
}