package br.com.medshare.comum;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Concentra o tratamento de erro da API inteira. Nenhum controller precisa de
 * try/catch: ele deixa a excecao subir e a traducao para HTTP acontece aqui.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RegraDeNegocioViolada.class)
    public ResponseEntity<RespostaDeErro> regraViolada(RegraDeNegocioViolada e) {
        return ResponseEntity.unprocessableEntity().body(RespostaDeErro.deRegra(
                422, "Regra de negócio violada", e.getMessage(), e.getRegra()));
    }

    @ExceptionHandler(RecursoNaoEncontrado.class)
    public ResponseEntity<RespostaDeErro> naoEncontrado(RecursoNaoEncontrado e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RespostaDeErro.de(404, "Não encontrado", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaDeErro> camposInvalidos(MethodArgumentNotValidException e) {
        List<RespostaDeErro.CampoInvalido> campos = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> new RespostaDeErro.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(RespostaDeErro.deCampos(400, "Requisição inválida", campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespostaDeErro> violacaoDeRestricao(ConstraintViolationException e) {
        return ResponseEntity.badRequest()
                .body(RespostaDeErro.de(400, "Requisição inválida", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaDeErro> acessoNegado(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RespostaDeErro.de(403, "Acesso negado",
                        "Seu perfil não tem permissão para esta operação"));
    }

    /**
     * Rede de seguranca: se um bug passar pela aplicacao, o banco barra e a
     * mensagem da constraint chega aqui. Tratamos como regra violada porque e
     * exatamente isso — so que detectada uma camada abaixo.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaDeErro> integridadeViolada(DataIntegrityViolationException e) {
        String causa = e.getMostSpecificCause().getMessage();
        log.warn("Restricao do banco barrou a operacao: {}", causa);
        return ResponseEntity.unprocessableEntity().body(RespostaDeErro.deRegra(
                422, "Regra de negócio violada", primeiraLinha(causa), regraCitadaEm(causa)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaDeErro> erroInesperado(Exception e) {
        log.error("Erro nao tratado", e);
        return ResponseEntity.internalServerError().body(RespostaDeErro.de(
                500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }

    private String primeiraLinha(String mensagem) {
        if (mensagem == null) {
            return "Operação rejeitada pelo banco de dados";
        }
        return mensagem.lines().findFirst().orElse(mensagem).trim();
    }

    /** Os gatilhos do banco comecam a mensagem com "RNxx:", entao da para extrair. */
    private String regraCitadaEm(String mensagem) {
        if (mensagem == null) {
            return null;
        }
        int inicio = mensagem.indexOf("RN");
        if (inicio < 0 || inicio + 4 > mensagem.length()) {
            return null;
        }
        String possivel = mensagem.substring(inicio, inicio + 4);
        return possivel.matches("RN\\d{2}") ? possivel : null;
    }
}
