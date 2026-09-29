package br.com.medshare.seguranca;

import br.com.medshare.seguranca.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/autenticacao")
public class ControladorDeAutenticacao {

    private final ServicoDeAutenticacao autenticacao;

    public ControladorDeAutenticacao(ServicoDeAutenticacao autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<RespostaDeLogin> cadastrar(@Valid @RequestBody PedidoDeCadastro pedido) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacao.cadastrar(pedido));
    }

    @PostMapping("/login")
    public RespostaDeLogin entrar(@Valid @RequestBody PedidoDeLogin pedido) {
        return autenticacao.entrar(pedido);
    }

    @PostMapping("/renovacao")
    public RespostaDeLogin renovar(@Valid @RequestBody PedidoDeRenovacao pedido) {
        return autenticacao.renovar(pedido.tokenDeRenovacao());
    }
}
