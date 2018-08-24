package xyz.mrcroxx.hgserver

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.google.gson.Gson
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class A1(
        @Id
        @Column(length = 128)
        var openid: String,
        @Column(length = 128)
        var school: String?,
        @Column(length = 64)
        var grade: String?,
        @Column(length = 128)
        var name: String?,
        @Column(length = 64)
        var sex: String?,
        @Column(length = 128)
        var sexto: String?,
        @Column(length = 2048)
        var description: String?,
        @Column(length = 128)
        var contact: String?,
        @Column(length = 128)
        var cpopenid: String?
) : Serializable {
    fun toJson(): String = Gson().toJson(this)
}

@JacksonXmlRootElement(localName = "xml")
data class WXMessage(
        @JacksonXmlProperty(localName = "ToUserName")
        val toUsername: String? = null,
        @JacksonXmlProperty(localName = "FromUserName")
        val fromUserName: String? = null,
        @JacksonXmlProperty(localName = "CreateTime")
        val createTime: Long? = null,
        @JacksonXmlProperty(localName = "MsgType")
        val msgType: String? = null,
        @JacksonXmlProperty(localName = "Content")
        val content: String? = null,
        @JacksonXmlProperty(localName = "MsgId")
        val msgId: Long? = null,
        @JacksonXmlProperty(localName = "Event")
        val event: String? = null
) : Serializable {
    fun toJson(): String = Gson().toJson(this)
}

fun String.toWXMessage(): WXMessage = Gson().fromJson(this, WXMessage::class.java)
fun String.toA1(): A1 = Gson().fromJson(this, A1::class.java)


@Configuration
@EnableAutoConfiguration
@ConfigurationProperties(prefix = "msg.emoji")
class EmojiFactory {
    val contents: List<String> = mutableListOf()
    /**
     *@Description  获取随机颜文字
     *@params  []
     *@return  随机颜文字:String
     *@Author  Croxx
     *@Date  2018/8/24
     */
    fun createEmoji(): String {
        val random = Random(System.currentTimeMillis())
        return contents[random.nextInt(contents.size)]
    }

    /**
     *@Description 返回当前时间戳
     *@params  []
     *@return  当前时间戳
     *@Author  Croxx
     *@Date  2018/8/23
     */
    fun getTimestamp(): Long = System.currentTimeMillis() / 1000
}