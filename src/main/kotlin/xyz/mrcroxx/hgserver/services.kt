package xyz.mrcroxx.hgserver


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import xyz.mrcroxx.hgserver.WXService.Companion.STATUS_DEFAULT
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


interface Act {
    val name: String
    val actName: String
    val entry: List<String>
    val description: String
    val timeStart: LocalDateTime
    val timeEnd: LocalDateTime
    fun hasStatus(value: String): Boolean
    fun handle(wxMessage: WXMessage, statusValue: String): WXMessage
}

/**
 *@Description 微信消息服务
 */
@Service
class WXService(@Autowired val redisService: RedisService,
                @Autowired val emojiFactory: EmojiFactory,
                @Autowired val act1CP: Act1CP,
                @Autowired val act2CPMission: Act2CPMission) {

    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_EVENT = "event"
        const val TYPE_IMAGE = "image"
        const val EVENT_SUBSCRIBE = "subscribe"

        const val STATUS_DEFAULT = "STATUS_DEFAULT"


    }

    @Value("\${wx.appid}")
    val appid: String? = null

    @Value("\${wx.token}")
    val token: String? = null

    @Value("\${msg.subscribe}")
    val subscribe: String? = null


    // TODO:Register Acts
    val acts: List<Act> = listOf(act1CP, act2CPMission)


    /**
     *@Description 处理微信发来的消息
     *@params  [wxMessage:接收消息]
     *@return  返回消息
     *@Author  Croxx
     *@Date  2018/8/23
     */
    fun handle(wxMessage: WXMessage): WXMessage {
        // 是否为Event消息
        if (wxMessage.msgType == TYPE_EVENT) return handleEvent(wxMessage)
        // 获取用户Status缓存
        val status = redisService.getStatus(wxMessage.fromUserName!!)
        val date = LocalDateTime.now()
        // 检查Status是否在某活动中
        acts.forEach {
            if (date > it.timeStart && date < it.timeEnd && it.hasStatus(status)) return it.handle(wxMessage, status)
        }
        // 初始化用户Status
        redisService.setStatus(wxMessage.fromUserName, STATUS_DEFAULT)
        // 检查是否为入口消息
        acts.forEach {
            if (wxMessage.msgType == TYPE_TEXT && wxMessage.content in it.entry)
                if (date < it.timeStart)
                    return echoBefore(wxMessage)
                else if (date > it.timeEnd)
                    return echoAfter(wxMessage)
                else
                    return it.handle(wxMessage, status)
        }
        return echoEmoji(wxMessage)
    }


    /**
     *@Description 处理Event类型消息
     *@params  [wxMessage:接收消息]
     *@return  返回消息
     *@Author  Croxx
     *@Date  2018/8/23
     */
    fun handleEvent(wxMessage: WXMessage): WXMessage {

        return when (wxMessage.event) {
            EVENT_SUBSCRIBE -> echoSubscribe(wxMessage)
            else -> echoSubscribe(wxMessage)
        }
    }

    /**
     *@Description 返回订阅消息
     *@params  [wxMessage:接收消息]
     *@return  返回消息
     *@Author  Croxx
     *@Date  2018/8/23
     */
    fun echoSubscribe(wxMessage: WXMessage): WXMessage {
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = TYPE_TEXT, content = subscribe)
    }

    /**
     *@Description 返回随机emoji消息
     *@params  [wxMessage:接收消息]
     *@return  返回消息
     *@Author  Croxx
     *@Date  2018/8/23
     */
    fun echoEmoji(wxMessage: WXMessage): WXMessage {

        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = TYPE_TEXT, content = emojiFactory.createEmoji())
    }

    fun echoBefore(wxMessage: WXMessage): WXMessage {
        return return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = TYPE_TEXT, content = """
                    |这项活动还未开始，请耐心等待呦~
                    |
                    |(｡･ω･｡)ﾉ♡""".trimMargin())
    }

    fun echoAfter(wxMessage: WXMessage): WXMessage {
        return return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = TYPE_TEXT, content = """
                    |这项活动已经结束，航概君会推出更多好玩的活动呦，敬请期待~
                    |
                    |∠( ᐛ 」∠)＿""".trimMargin())
    }


}

/**
 *@Description Redis缓存服务
 */
@Service
class RedisService(@Autowired val stringRedisTemplate: StringRedisTemplate) {

    @Value("\${redis.expire}")
    val expire: Long? = null

    @Value("\${redis.status-prefix}")
    val statusPrefix: String? = null

    fun setStatus(openid: String, status: String) {
        val key = statusPrefix + openid
        stringRedisTemplate.opsForValue().set(key, status)
        stringRedisTemplate.expire(key, expire!!, TimeUnit.SECONDS)
    }

    fun getStatus(openid: String): String = stringRedisTemplate.opsForValue().get(statusPrefix + openid)
            ?: STATUS_DEFAULT

    fun setActData(actName: String, openid: String, data: String, expire: Long) {
        val key = "$actName::$openid"
        stringRedisTemplate.opsForValue().set(key, data)
        if (expire > 0) stringRedisTemplate.expire(key, expire, TimeUnit.SECONDS)
    }

    fun getActData(actName: String, openid: String): String? = stringRedisTemplate.opsForValue().get("$actName::$openid")

}

@Service
class FileService(@Autowired val restTemplate: RestTemplate,
                  @Autowired val resourceLoader: ResourceLoader) {

    @Value("\${wx.upload-dir}")
    val uploaddir: String? = null

    fun saveImageFromUrl(url: String, dirpath: String, name: String, type: String = ".jpg"): Boolean {
        val imageBytes: ByteArray = restTemplate.getForObject(url, ByteArray::class.java) ?: return false
        val path = Paths.get(uploaddir, dirpath)
        if (Files.notExists(path))
            Files.createDirectories(path)
        Files.write(Paths.get(path.toString(), "$name$type"), imageBytes)
        return true
    }
}