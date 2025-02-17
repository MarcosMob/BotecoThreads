package boteco.cliente;

import boteco.Boteco;

public class Cliente {
    private final String nome;
    private final Boteco boteco;

    public Cliente(String nome, Boteco boteco) {
        this.nome = nome;
        this.boteco = boteco;
    }

    public String getNome() {
        return nome;
    }

    public Boteco getBoteco() {
        return boteco;
    }

    public void sair() {
        //tempo que o cleinte leva pra sair
        try {
            Thread.sleep((long) (Math.random() * 10000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String toString() {
        return nome;
    }
}