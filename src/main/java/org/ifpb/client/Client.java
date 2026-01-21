package org.ifpb.client;

import org.ifpb.shared.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORTA = 12345;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Scanner MsgIdScanner = new Scanner(System.in);
        System.out.println("=== Cliente de Mensagens Sockets ===");

        System.out.print("Modo de Conexão? (P)ersistente ou (T)ransiente: ");
        String modo = scanner.next();
        /**
         * aqui estamos verificando se o modo escolhido pelo usuario e persistente ou transiente
         *
         * 1. Modo Persistente (Keep-Alive) é aberta a varias requisiçoes sem fechar a conexão, so quando o cliente disser (Ei vou ali visse, valeuu)
         *
         * 2. Modo Transiente (Non-Persistent)É como mandar uma carta para cada pergunta. Você escreve, envia, recebe a resposta e joga o envelope fora(greta thumberg chora).
         * Para a próxima pergunta, precisa de um novo envelope (nova conexão) ou seja um new socket kkkk.
         * */
        boolean isPersistente = modo.equalsIgnoreCase("P");

        // Flag do protocolo
        String flagConexao = isPersistente ? Protocol.TIPO_PERSISTENTE : Protocol.TIPO_TRANSIENTE;

        Socket socket = null;
        DataOutputStream saida = null;
        InputStream entrada = null;

        try {
            // Se for persistente, conecta UMA vez aqui fora
            if (isPersistente) {
                socket = new Socket(HOST, PORTA);
                saida = new DataOutputStream(socket.getOutputStream());
                entrada = socket.getInputStream();
                System.out.println(">> Conexão Persistente Estabelecida <<");
            }

            while (true) {
                System.out.print("\nDigite ID da mensagem (0=Aleatória, -1=Sair): ");
                int id = Integer.parseInt(MsgIdScanner.nextLine());

                if (id == -1) break;

                // Se for transiente (ou se a conexão caiu), conecta AGORA
                if (!isPersistente || socket.isClosed()) {
                    socket = new Socket(HOST, PORTA);
                    saida = new DataOutputStream(socket.getOutputStream());
                    entrada = socket.getInputStream();
                }

                // 1. Envia Requisição
                String msg = id + Protocol.SEPARADOR + flagConexao + "\n"; // \n é importante para o readLine do servidor
                saida.write(msg.getBytes());
                saida.flush();

                // 2. Recebe Resposta de Tamanho Fixo
                byte[] buffer = new byte[Protocol.TAMANHO_RESPOSTA];
                int lidos = entrada.read(buffer);

                if (lidos == -1) {
                    System.out.println("Servidor fechou a conexão.");
                    break;
                }

                // 3. Processa e Limpa os espaços vazios
                String respostaCheia = new String(buffer, 0, lidos); // Pega o que veio
                String[] dados = respostaCheia.trim().split(Protocol.SEPARADOR, 2);

                String status = dados[0];
                String texto = (dados.length > 1) ? dados[1] : "";

                System.out.println("Status: [" + status + "] | Mensagem: " + texto);

                // Se for transiente, fecha AGORA
                if (!isPersistente) {
                    socket.close();
                }
            }

        } catch (IOException e) {
            System.err.println("Erro de conexão: " + e.getMessage());
        } finally {
            try { if (socket != null) socket.close(); } catch (Exception e) {}
            scanner.close();
        }
    }
}