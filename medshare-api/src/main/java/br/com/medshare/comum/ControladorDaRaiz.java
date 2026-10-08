package br.com.medshare.comum;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * O endereco puro da API, http://localhost:8081/.
 *
 * Nao ha pagina aqui — a API so responde em /api — e sem este controlador
 * quem abria o endereco no navegador recebia um 401 seco, que parecia a API
 * quebrada. Agora cai na documentacao, que e o que faz sentido abrir aqui.
 */
@Controller
public class ControladorDaRaiz {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/swagger-ui/index.html";
    }
}
