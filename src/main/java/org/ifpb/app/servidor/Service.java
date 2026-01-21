package org.ifpb.app.servidor;

import org.ifpb.shared.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Service {
    private static final int PORTA = 12345;
    private static final String ARQUIVO_MENSAGENS = "mensagens.txt";
    private static List<String> mensagens = new ArrayList<>();

    // Thread Pool: Gerencia as threads de forma eficiente
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println(">>> Iniciando Servidor de Mensagens <<<");
        if (!carregarMensagens()) {
            System.err.println("pani no sistema meu irmão: Não foi possível carregar mensagens. Encerrando, bye bye marabá!.");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Servidor ouvindo na porta " + PORTA);
            System.out.println("Aguardando conexões...");

            while (true) {
                Socket clienteSocket = serverSocket.accept();
                System.out.println("[Conexão Nova] IP: " + clienteSocket.getInetAddress());

                // Submete a tarefa para o pool de threads em vez de criar uma "new Thread" solta
                threadPool.execute(new ClientHandler(clienteSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean carregarMensagens() {
        File arquivo = new File(ARQUIVO_MENSAGENS);
        if (!arquivo.exists()) {
            System.err.println("ERRO: Arquivo " + ARQUIVO_MENSAGENS + " não encontrado na raiz: " + arquivo.getAbsolutePath());
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linhaQtd = br.readLine();
            if (linhaQtd == null) return false;

            int quantidadeInformada = Integer.parseInt(linhaQtd.trim());

            String linha;
            while ((linha = br.readLine()) != null) {
                if (!linha.trim().isEmpty()) {
                    mensagens.add(linha);
                }
            }

            System.out.println("Mensagens carregadas: " + mensagens.size() + " (Esperado: " + quantidadeInformada + ")");

            if (mensagens.isEmpty()) return false;

            return true;

        } catch (Exception e) {
            System.err.println("Erro ao ler arquivo meu patrão: " + e.getMessage());
            return false;
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    DataOutputStream saida = new DataOutputStream(socket.getOutputStream())
            ) {
                boolean manterConexao = true;

                while (manterConexao) {
                    String requisicao = entrada.readLine();
                    if (requisicao == null) break; // Cliente fechou conexão

                    // Log simples para o servidor
                    System.out.println("[REQ] Cliente " + socket.getPort() + ": " + requisicao);

                    // Parse da requisição
                    String[] partes = requisicao.split(Protocol.SEPARADOR);
                    if (partes.length < 2) continue; // Ignora lixo

                    int id = Integer.parseInt(partes[0]);
                    String tipoConexao = partes[1];

                    // Processa Lógica
                    String respostaFormatada = processarLogica(id);

                    // Envia Bytes Fixos
                    byte[] buffer = respostaFormatada.getBytes();
                    saida.write(buffer); // Envia array completo (já formatado com espaços)
                    saida.flush();

                    // Verifica desconexão transiente
                    if (tipoConexao.equalsIgnoreCase(Protocol.TIPO_TRANSIENTE)) {
                        manterConexao = false;
                        System.out.println("[INFO] Fechando conexão transiente com " + socket.getPort());
                    }
                }
            } catch (SocketException e) {
                System.out.println("[INFO] Cliente desconectou abruptamente(igual um cavalo).");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }

        private String processarLogica(int id) {
            String conteudo;
            String status = Protocol.STATUS_OK;
            int total = mensagens.size();

            if (id == 0) {
                conteudo = mensagens.get(new Random().nextInt(total));
            } else if (id < 1 || id > total) {
                status = Protocol.STATUS_ERRO;
                conteudo = "ID Invalido. Use 1 a " + total;
            } else {
                conteudo = mensagens.get(id - 1);
            }

            // Usa o método auxiliar do Protocolo
            return Protocol.formatarResposta(status, conteudo);
        }
    }
}

