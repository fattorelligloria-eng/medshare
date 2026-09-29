package br.com.medshare.usuario;

/**
 * Uma pessoa pode acumular papeis: quem doou um medicamento hoje pode precisar
 * de outro amanha. Por isso papel e lista, nao um campo "tipo" unico.
 */
public enum Papel {
    DOADOR,
    BENEFICIARIO,
    FARMACEUTICO,
    ADMIN;

    /** O Spring Security espera os papeis no formato ROLE_X. */
    public String comoAutoridade() {
        return "ROLE_" + name();
    }
}
