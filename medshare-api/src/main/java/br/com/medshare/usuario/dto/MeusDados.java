package br.com.medshare.usuario.dto;

import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Os dados da propria pessoa, para a tela de conta.
 *
 * O CPF vai mascarado. Ela ja sabe o numero dela; o que a tela precisa e
 * confirmar que o cadastro esta certo, e para isso os ultimos digitos bastam.
 * Trafegar o CPF inteiro a cada abertura de tela e risco sem ganho.
 *
 * A foto vem como link assinado e com prazo (nunca o endereco gravado), e a
 * validade do CadUnico so aparece enquanto a verificacao estiver vigente: e o
 * que a carteirinha digital mostra.
 */
public record MeusDados(
        String nome,
        String cpfMascarado,
        String email,
        String telefone,
        EnderecoDaConta endereco,
        List<String> papeis,
        OffsetDateTime membroDesde,
        String fotoUrl,
        String numeroCarteirinha,
        LocalDate cadUnicoValidoAte
) {

    public record EnderecoDaConta(String cep, String logradouro, String numero,
                                  String complemento, String bairro,
                                  Short municipioId, String municipio) { }

    public static MeusDados de(Usuario usuario, String fotoAssinada, LocalDate cadUnicoValidoAte) {
        var e = usuario.getEndereco();
        return new MeusDados(
                usuario.getNome(),
                mascarar(usuario.getCpf()),
                usuario.getEmail(),
                usuario.getTelefone(),
                e == null ? null : new EnderecoDaConta(
                        e.getCep(), e.getLogradouro(), e.getNumero(), e.getComplemento(),
                        e.getBairro(),
                        e.getMunicipio() == null ? null : e.getMunicipio().getId(),
                        e.getMunicipio() == null ? null : e.getMunicipio().getNome()),
                usuario.getPapeis().stream().map(Papel::name).sorted().toList(),
                usuario.getCriadoEm(),
                fotoAssinada,
                usuario.getNumeroCarteirinha(),
                cadUnicoValidoAte);
    }

    /** 12345678901 vira •••.•••.789-01. */
    static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) return null;
        return "•••.•••." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
