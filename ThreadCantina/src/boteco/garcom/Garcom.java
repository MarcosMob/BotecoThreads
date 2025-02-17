package boteco.garcom;

import boteco.Boteco;
import boteco.mesa.Mesa;

public class Garcom implements Runnable {
    private final String nome;
    private final Boteco boteco;

    public Garcom(String nome, Boteco boteco) {
        this.nome = nome;
        this.boteco = boteco;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //tempo para o garçom verificar mesas sujas
                Thread.sleep((long) (Math.random() * 3000));

                Mesa mesaParaLimpar = boteco.encontrarMesaParaLimpar();
                if (mesaParaLimpar != null) {
                    mesaParaLimpar.getLock().lock();
                    try {
                        if (!mesaParaLimpar.estaReservada()) {
                            mesaParaLimpar.reservarGarcom(this);
                            //reserva a mesa para o garcom
                        }

                        //espera ate que a mesa possa ta vazia e suja
                        while (!mesaParaLimpar.estaVaziaParaLimpeza()) {
                            mesaParaLimpar.getMesaDisponivel().await();
                        }
                        mesaParaLimpar.limpar(this);
                    } finally {
                        mesaParaLimpar.liberarReservaGarcom();
                        //mesa fica livre
                        mesaParaLimpar.getLock().unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}