package boteco;

import boteco.cliente.Cliente;
import boteco.mesa.Mesa;
import boteco.garcom.Garcom;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class Boteco {
    private final List<Mesa> mesas;
    private final List<Garcom> garcons;
    private final List<Cliente> filaClientes;
    private final Random random = new Random();

    public Boteco(int numMesas, int numGarcons) {
        mesas = new ArrayList<>();
        garcons = new ArrayList<>();
        filaClientes = new ArrayList<>();

        for (int i = 0; i < numMesas; i++) {
            mesas.add(new Mesa(i + 1, 4));  // Cada mesa pode ter até 4 clientes
        }

        for (int i = 0; i < numGarcons; i++) {
            garcons.add(new Garcom("Garçom " + (i + 1), this));
        }
    }

    public List<Mesa> getMesas() {
        return mesas;
    }

    public Mesa encontrarMesaParaLimpar() {
        synchronized (mesas) {
            for (Mesa mesa : mesas) {
                if (mesa.isSuja() && !mesa.isSendoLimpa() && !mesa.estaReservada()) {
                    return mesa;
                }
            }
        }
        return null;
    }


    public void adicionarCliente(Cliente cliente) {
        synchronized (filaClientes) {
            filaClientes.add(cliente);
        }
    }

    public void iniciarServico() {
        //incializa os garçons
        for (Garcom garcom : garcons) {
            new Thread(garcom).start();
        }

        //uso dos garcons para limpar as mesas de tempo em tempo
        acionarGarcomRegularmente();

        //  clientes entrando e tentando sentar nas mesas
        new Thread(() -> {
            while (true) {
                synchronized (filaClientes) {
                    if (!filaClientes.isEmpty()) {
                        // pega um cliente aleatoriamente da fila
                        int index = random.nextInt(filaClientes.size());
                        Cliente cliente = filaClientes.remove(index);

                        boolean sentou = false;
                        for (Mesa mesa : mesas) {
                            mesa.getLock().lock();
                            try {
                                if (mesa.getOcupacaoAtual() < mesa.getCapacidade() && !mesa.isSuja() && !mesa.isSendoLimpa()) {
                                    mesa.sentar(cliente);
                                    sentou = true;
                                    break;
                                }
                            } finally {
                                mesa.getLock().unlock();
                            }
                        }

                        if (!sentou) {
                            System.out.println(cliente.getNome() + " está esperando por uma mesa.");
                            filaClientes.add(cliente);
                            //  não conseguiu sentar adiciona a fila
                        }
                    }
                }
                try {
                    Thread.sleep((long) (Math.random() * 3000)); //tempo de chegada de clientes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        //simulacao clientes saindo da mesa
        new Thread(this::sairClientes).start();
    }


    private void sairClientes() {
        Random random = new Random();
        while (true) {
            synchronized (mesas) {
                for (Mesa mesa : mesas) {
                    mesa.getLock().lock();
                    try {
                        if (mesa.getOcupacaoAtual() > 0) {
                            List<Cliente> clientes = mesa.getClientes();
                            if (!clientes.isEmpty()) {
                                Cliente cliente = clientes.get(random.nextInt(clientes.size()));
                                mesa.sair(cliente);
                            }
                        }
                    } finally {
                        mesa.getLock().unlock();
                    }
                }
            }

            try {
                Thread.sleep((long) (Math.random() * 5000));
                // tempo aleatorio para simular saída de clientes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //restaura o estado de interrupcao
            }
        }
    }

    private void acionarGarcomRegularmente() {
        new Thread(() -> {
            while (true) {
                try {
                    int delay = 1000 + random.nextInt(6000);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                Garcom garcomSelecionado = garcons.get(random.nextInt(garcons.size()));
                Mesa mesaParaLimpar = encontrarMesaParaLimpar();

                if (mesaParaLimpar != null) {
                    new Thread(() -> {
                        mesaParaLimpar.getLock().lock();
                        try {
                            if (!mesaParaLimpar.isSendoLimpa() && mesaParaLimpar.isSuja() && !mesaParaLimpar.estaReservada()) {
                                mesaParaLimpar.reservarGarcom(garcomSelecionado);
                                mesaParaLimpar.setSendoLimpa(true);
                                System.out.println("Garçom " + garcomSelecionado.getNome() + " foi acionado para limpar a mesa " + mesaParaLimpar.getNumero() + ".");

                                while (mesaParaLimpar.getOcupacaoAtual() > 0) {
                                    System.out.println("Garçom " + garcomSelecionado.getNome() + " está aguardando a mesa " + mesaParaLimpar.getNumero() + " desocupar.");
                                    mesaParaLimpar.getMesaDisponivel().await();
                                }
                                mesaParaLimpar.limpar(garcomSelecionado);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            mesaParaLimpar.liberarReservaGarcom();
                            mesaParaLimpar.setSendoLimpa(false);
                            mesaParaLimpar.getLock().unlock();
                        }
                    }).start();
                }
            }
        }).start();
    }


    public static void main(String[] args) {
        int numClientes = 60;

        Boteco boteco = new Boteco(1, 3);

        // Iniciando o serviço
        boteco.iniciarServico();

        // Adicionando clientes na fila
        for (int i = 0; i < numClientes; i++) {
            boteco.adicionarCliente(new Cliente("Cliente " + (i + 1), boteco));
        }
    }
}