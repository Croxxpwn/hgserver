package xyz.mrcroxx.hgserver

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping
class WXcontroller(@Autowired val wxService: WXService) {

    @GetMapping("/wx")
    fun test(@RequestParam("signature") signature: String,
             @RequestParam("timestamp") timestamp: String,
             @RequestParam("nonce") nonce: String,
             @RequestParam("echostr") echostr: String): String {
        val keys = mutableListOf(wxService.token, timestamp, nonce)
        keys.sort()
        val plain = keys[0] + keys[1] + keys[2]
        val code = DigestUtils.sha1(plain).toString()
        if (code == signature) {
            return echostr
        }
        return "false"
    }
}