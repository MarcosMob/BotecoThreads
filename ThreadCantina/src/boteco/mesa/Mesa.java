package boteco.mesa;

import boteco.cliente.Cliente;
import boteco.garcom.Garcom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Mesa {
    private final int numero;
    private final int capacidade;
    private final List<Cliente> clientes;
    private boolean suja;
    private boolean sendoLimpa;
    private Garcom garcomDesignado;
    private Garcom garcomReservado;

    private final Lock lock = new ReentrantLock();
    private final Condition mesaDisponivel = lock.newCondition();

    public Mesa(int numero, int capacidade) {
        this.numero = numero;
        this.capacidade = capacidade;
        this.clientes = new ArrayList<>();
        this.suja = false;
        this.sendoLimpa = false;
        this.garcomDesignado = null;
    }

    public int getNumero() {
        return numero;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public int getOcupacaoAtual() {
        return clientes.size();
    }

    public boolean isSuja() {
        return suja;
    }

    public boolean isSendoLimpa() {
        return sendoLimpa;
    }

    public void setSendoLimpa(boolean sendoLimpa) {
        this.sendoLimpa = sendoLimpa;
    }

    public Garcom getGarcomReservado() {
        return garcomReservado;
    }

    public void reservarGarcom(Garcom garcom) {
        this.garcomReservado = garcom;
    }

    public void liberarReservaGarcom() {
        this.garcomReservado = null;
    }

    // Método para verificar se a mesa está reservada
    public boolean estaReservada() {
        return garcomReservado != null;
    }

    public List<Cliente> getClientes() {
        return clientes;
    }

    public void sentar(Cliente cliente) {
        lock.lock();
        try {
            if (clientes.size() < capacidade && !suja && !sendoLimpa) {
                clientes.add(cliente);
                System.out.println(cliente.getNome() + " sentou-se na mesa " + numero + ". Ocupação: " + getOcupacaoAtual() + "/" + capacidade);
                if (clientes.size() == capacidade) {
                    suja = true;
                }
                mesaDisponivel.signalAll();
                //notifica que a mesa pode estar disponível para limpeza
            }
        } finally {
            lock.unlock();
        }
    }
    public void sair(Cliente cliente) {
        lock.lock();
        try {
            if (clientes.remove(cliente)) {
                System.out.println(cliente.getNome() + " saiu da mesa " + numero + ". Ocupação: " + getOcupacaoAtual() + "/" + capacidade);
                if (clientes.isEmpty()) {
                    suja = true;
                    mesaDisponivel.signalAll();
                    //notifica que a mesa pode estar disponivel para limpeza
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean estaVaziaParaLimpeza() {
        return clientes.isEmpty() && suja;
    }

    public void limpar(Garcom garcom) {
        lock.lock();
        try {
            if (!sendoLimpa && suja) {
                sendoLimpa = true;
                garcomDesignado = garcom;
                System.out.println(garcom.getNome() + " está limpando a mesa " + numero + ".");
                try {
                    Thread.sleep(2000); // Simula o tempo de limpeza
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                suja = false;
                System.out.println("Mesa " + numero + " foi limpa por " + garcom.getNome() + ".");
                sendoLimpa = false;
                //marca como nao sendo mais limpa
            }
        } finally {
            lock.unlock();
        }
    }

    public Lock getLock() {
        return lock;
    }

    public Condition getMesaDisponivel() {
        return mesaDisponivel;
    }
}