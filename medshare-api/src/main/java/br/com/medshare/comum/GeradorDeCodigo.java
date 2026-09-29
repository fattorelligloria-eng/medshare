package br.com.medshare.comum;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Codigos que aparecem para o usuario (o da doacao e o de retirada).
 *
 * Sem I, O, 0 e 1 no alfabeto: esses codigos sao ditados no balcao da farmacia
 * e lidos de uma tela de celular, onde zero e letra O viram a mesma coisa.
 * SecureRandom porque o codigo de retirada autoriza pegar o medicamento —
 * se fosse adivinhavel, seria uma falha de seguranca.
 */
@Component
public class GeradorDeCodigo {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SORTEIO = new SecureRandom();

    public String paraDoacao() {
        return "MS-" + sortear(6);
    }

    public String paraRetirada() {
        return sortear(8);
    }

    private String sortear(int tamanho) {
        StringBuilder codigo = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            codigo.append(ALFABETO.charAt(SORTEIO.nextInt(ALFABETO.length())));
        }
        return codigo.toString();
    }
}
