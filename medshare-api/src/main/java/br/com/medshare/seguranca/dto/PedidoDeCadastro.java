package br.com.medshare.seguranca.dto;

import br.com.medshare.usuario.Papel;
import jakarta.validation.constraints.*;

import java.util.Set;

public record PedidoDeCadastro(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "informe os 11 dígitos do CPF, sem pontos") String cpf,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "a senha precisa de pelo menos 8 caracteres") String senha,
        String telefone,

        @NotBlank @Pattern(regexp = "\\d{8}", message = "informe os 8 dígitos do CEP, sem traço") String cep,
        @NotBlank String logradouro,
        @NotBlank String numero,
        String complemento,
        @NotBlank String bairro,
        @NotNull Short municipioId,
        Double latitude,
        Double longitude,

        @NotEmpty Set<Papel> papeis
) { }
