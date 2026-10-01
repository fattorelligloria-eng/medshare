package br.com.medshare.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O que a pessoa pode mudar sozinha. Sem CPF e sem e-mail, de proposito. */
public record PedidoDeAtualizacaoDeCadastro(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 20) String telefone,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "informe os 8 dígitos do CEP, sem traço")
        String cep,
        @NotBlank @Size(max = 160) String logradouro,
        @NotBlank @Size(max = 20) String numero,
        @Size(max = 60) String complemento,
        @NotBlank @Size(max = 80) String bairro,
        @NotNull Short municipioId,
        Double latitude,
        Double longitude
) { }
