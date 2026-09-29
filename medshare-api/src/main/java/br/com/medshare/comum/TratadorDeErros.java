package br.com.medshare.comum;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
        String regra = regraCitadaEm(causa);
        // So a mensagem dos nossos gatilhos (que comecam com RNxx) vai para o
        // cliente. O resto e texto do Postgres, com nome de tabela e coluna.
        String mensagem = regra != null
                ? primeiraLinha(causa)
                : "Os dados enviados conflitam com um registro existente";
        return ResponseEntity.unprocessableEntity().body(RespostaDeErro.deRegra(
                422, "Regra de negócio violada", mensagem, regra));
    }

    /** Corpo que nao e JSON valido, ou campo com tipo errado (data mal escrita, etc.). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaDeErro> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(RespostaDeErro.de(400, "Requisição inválida",
                "O corpo da requisição não pôde ser lido. Confira o formato dos campos."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaDeErro> parametroComTipoErrado(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(RespostaDeErro.de(400, "Requisição inválida",
                "O valor informado em '%s' não é válido".formatted(e.getName())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespostaDeErro> arquivoGrandeDemais(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(RespostaDeErro.deRegra(413,
                "Arquivo grande demais", "A foto passa de 8 MB. Tire a foto em resolução menor.", "FOTO"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaDeErro> erroInesperado(Exception e) {
        // Erros do proprio Spring MVC (rota inexistente, metodo nao suportado,
        // parametro faltando...) ja sabem o status certo. Antes todos viravam 500.
        if (e instanceof ErrorResponse respostaDoSpring) {
            HttpStatusCode status = respostaDoSpring.getStatusCode();
            String detalhe = respostaDoSpring.getBody().getDetail();
            return ResponseEntity.status(status).body(RespostaDeErro.de(status.value(),
                    "Requisição inválida", detalhe != null ? detalhe : "Requisição inválida"));
        }
        log.error("Erro nao tratado", e);
        return ResponseEntity.internalServerError().body(RespostaDeErro.de(
                500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }

    private String primeiraLinha(String mensagem) {
        return mensagem.lines().findFirst().orElse(mensagem).replaceFirst("^ERROR:\\s*", "").trim();
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
