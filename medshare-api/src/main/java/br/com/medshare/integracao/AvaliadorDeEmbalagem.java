package br.com.medshare.integracao;

/**
 * Quem olha a foto da caixa.
 *
 * A interface existe para que o resto do sistema nao saiba se por tras esta o
 * Gemini, um modelo proprio em TensorFlow Lite (fase 2) ou um simulador de
 * desenvolvimento. Trocar de avaliador nao deve encostar em nenhuma regra de
 * negocio.
 */
public interface AvaliadorDeEmbalagem {

    LeituraDaEmbalagem avaliar(String fotoUrl);

    /** Identificacao gravada na analise, para sabermos depois quem leu o que. */
    String nome();
}
