package sionic.demo

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/demo")
class DemoController {

    @GetMapping
    fun index(): String {
        return "demo/index"
    }

    @GetMapping("/login")
    fun login(): String {
        return "demo/login"
    }

    @GetMapping("/chat")
    fun chat(): String {
        return "demo/chat"
    }

    @GetMapping("/admin")
    fun admin(): String {
        return "demo/admin"
    }
}
