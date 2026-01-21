package org.ifpb.shared;

public class Protocol {
    public static final int TAMANHO_RESPOSTA = 128; // Bytes fixos
    public static final String SEPARADOR = ";";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERRO = "ERRO";
    public static final String TIPO_PERSISTENTE = "K"; // Keep-Alive
    public static final String TIPO_TRANSIENTE = "C";  // Close

    // Método utilitário para garantir o tamanho fixo (Padding)
    public static String formatarResposta(String status, String mensagem) {
        String completa = status + SEPARADOR + mensagem;
        // %-128s significa: alinhar à esquerda e preencher com espaços até 128 chars
        String formatada = String.format("%-" + TAMANHO_RESPOSTA + "s", completa);

        // Garante corte se passar de 128 (segurança)
        if (formatada.length() > TAMANHO_RESPOSTA) {
            return formatada.substring(0, TAMANHO_RESPOSTA);
        }
        return formatada;
    }
}
