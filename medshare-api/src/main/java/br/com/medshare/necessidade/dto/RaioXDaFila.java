package br.com.medshare.necessidade.dto;

/**
 * O que dá para dizer com honestidade sobre a espera de um pedido.
 *
 * Aqui falta de propósito a coisa que todo mundo esperaria: a posição na fila.
 * Ela não existe neste sistema. O matching ordena por prioridade, depois pela
 * faixa de distância até a farmácia onde a caixa apareceu, e só então pelo
 * tempo de espera — ou seja, a ordem só fica definida quando existe uma caixa
 * concreta num lugar concreto. A mesma pessoa é a segunda para uma caixa que
 * chega perto dela e a nona para uma que chega do outro lado da cidade.
 *
 * Mostrar "você é o 3º" seria um número que pula sozinho, por um motivo que a
 * pessoa não vê. Em vez disso vão os fatos que são estáveis e verdadeiros, e a
 * tela explica como a ordem funciona de verdade.
 *
 * @param pessoasNaFila        quantas esperam pelo mesmo princípio ativo
 * @param entraramDepoisDeVoce dessas, quantas chegaram depois — o comparativo
 *                             honesto, já que a antiguidade é um dos critérios
 * @param diasEsperando        há quantos dias o pedido existe
 * @param caixasRecentes       quantas caixas desse princípio ativo entraram no
 *                             estoque na janela observada
 * @param diasDesdeAUltima     há quantos dias foi a última; nulo se não houve
 * @param diasDaJanela         o tamanho da janela, para a tela poder dizer
 * @param prioridade           UC07 A2 — perdeu uma caixa por validade e passa à frente
 */
public record RaioXDaFila(
        long pessoasNaFila,
        long entraramDepoisDeVoce,
        long diasEsperando,
        int caixasRecentes,
        Long diasDesdeAUltima,
        int diasDaJanela,
        boolean prioridade
) { }
