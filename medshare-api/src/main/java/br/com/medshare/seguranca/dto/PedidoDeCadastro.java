package br.com.medshare.seguranca.dto;

import br.com.medshare.usuario.Papel;
import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * Os limites de tamanho acompanham as colunas da tabela usuario: sem eles, um
 * texto grande demais so era barrado pelo banco, com mensagem de SQL.
 */
public record PedidoDeCadastro(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "informe os 11 dígitos do CPF, sem pontos") String cpf,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72, message = "a senha precisa ter entre 8 e 72 caracteres") String senha,
        @Size(max = 20) String telefone,

        @NotBlank @Pattern(regexp = "\\d{8}", message = "informe os 8 dígitos do CEP, sem traço") String cep,
        @NotBlank @Size(max = 160) String logradouro,
        @NotBlank @Size(max = 20) String numero,
        @Size(max = 60) String complemento,
        @NotBlank @Size(max = 80) String bairro,
        @NotNull Short municipioId,
        Double latitude,
        Double longitude,

        /** Apenas DOADOR e BENEFICIARIO; os demais papeis sao dados pela equipe. */
        @NotEmpty Set<Papel> papeis
) { }
