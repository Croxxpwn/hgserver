package xyz.mrcroxx.hgserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.mrcroxx.hgserver.WXService.Companion.STATUS_DEFAULT
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author Croxx
 * @Date 2018/8/23
 * @Description
 */

@Service
@Transactional
class Act1CP(@Autowired val redisService: RedisService,
             @Autowired val a1Repository: A1Repository,
             @Autowired val emojiFactory: EmojiFactory) : Act {
    override val name: String
        get() = "A1"
    override val actName: String
        get() = "一周CP"
    override val entry: List<String>
        get() = listOf("cp", "CP")
    override val description: String
        get() = "一周CP"
    override val timeStart: Date
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-01 00:00:00")
    override val timeEnd: Date
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-10-01 00:00:00")

    enum class Status(val value: String) {
        DEFAULT("ACT1_DEFAULT"),
        HINT("ACT1_HINT"),
        FILL_SCHOOL("ACT1_FILL_SCHOOL"),
        FILL_GRADE("ACT1_FILL_GRADE"),
        FILL_NAME("ACT1_FILL_NAME"),
        FILL_SEX("ACT1_FILL_SEX"),
        FILL_SEXTO("ACT1_FILL_SEXTO"),
        FILL_DESCRIPTION("ACT1_FILL_DESCRIPTION"),
        FILL_CONTACT("ACT1_FILL_CONTACT"),
        FILL_CONFIRM("ACT1_FILL_CONFIRM");

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromString(value: String) = map[value]
            fun hasStatus(value: String): Boolean = map.contains(value)
        }
    }

    override fun hasStatus(value: String): Boolean {
        return Status.hasStatus(value)
    }


    override fun handle(wxMessage: WXMessage, statusValue: String): WXMessage {
        val status: Status = if (statusValue == STATUS_DEFAULT) Status.DEFAULT else Status.fromString(statusValue)!!
        return when (status) {
            Status.DEFAULT -> handleDefault(wxMessage)
            Status.HINT -> handleHint(wxMessage)
            Status.FILL_SCHOOL -> handleFillSchool(wxMessage)
            Status.FILL_GRADE -> handleFillGrade(wxMessage)
            Status.FILL_NAME -> handleFillName(wxMessage)
            Status.FILL_SEX -> handleFillSex(wxMessage)
            Status.FILL_SEXTO -> handleFillSexTo(wxMessage)
            Status.FILL_DESCRIPTION -> handleFillDescription(wxMessage)
            Status.FILL_CONTACT -> handleFillContact(wxMessage)
            Status.FILL_CONFIRM -> handleFillConfirm(wxMessage)
        }
    }

    fun handleDefault(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName
        val data: A1? = a1Repository.findOne(wxMessage.fromUserName!!)
        if (data != null) {
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |您已报名航概·一周CP活动
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |请等待匹配结果公布哦~
                """.trimMargin())
        } else {
            redisService.setStatus(openid!!, Status.HINT.value)
            return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                    msgType = WXService.TYPE_TEXT, content = """
                    |确定参加航概的一周CP活动吗？
                    |
                    |回复[报名] 回复[取消]
                """.trimMargin())
        }
    }

    fun handleHint(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        when (wxMessage.content) {
            "报名" -> {
                redisService.setStatus(openid, Status.FILL_SCHOOL.value)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的学校名~
                """.trimMargin())
            }
            "取消" -> {
                redisService.setStatus(openid, STATUS_DEFAULT)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |期待下次遇见你~
                """.trimMargin())
            }
            else -> {
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |确定参加航概的一周CP活动吗？
                    |
                    |回复[报名] 回复[取消]
                """.trimMargin())
            }
        }
    }

    fun handleFillSchool(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1()
                ?: A1(openid, null, null, null, null, null, null, null, null)
        data.school = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_GRADE.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的年级~
                """.trimMargin())
    }

    fun handleFillGrade(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.grade = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_NAME.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的名字~
                """.trimMargin())
    }

    fun handleFillName(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.name = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_SEX.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的性别~
                """.trimMargin())
    }

    fun handleFillSex(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.sex = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_SEXTO.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的取向~
                """.trimMargin())
    }

    fun handleFillSexTo(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.sexto = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_DESCRIPTION.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的一句话简介~
                """.trimMargin())
    }

    fun handleFillDescription(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.description = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_CONTACT.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请回复你的联系方式~
                """.trimMargin())
    }

    fun handleFillContact(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        data.contact = wxMessage.content
        redisService.setActData(name, openid, data.toJson(), -1)
        redisService.setStatus(openid, Status.FILL_CONFIRM.value)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |请核对您的报名信息~
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |回复[确定] 回复[取消]
                """.trimMargin())
    }

    fun handleFillConfirm(wxMessage: WXMessage): WXMessage {
        val openid = wxMessage.fromUserName!!
        val data = redisService.getActData(name, openid)?.toA1() ?: return handleError(wxMessage)
        when (wxMessage.content) {
            "确定" -> {
                a1Repository.save(data)
                redisService.setStatus(openid, STATUS_DEFAULT)
                redisService.setActData(name, openid, "", 1)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |报名成功~您的报名信息如下：
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |请耐心等待匹配结果公布~
                """.trimMargin())
            }
            "取消" -> {
                redisService.setStatus(openid, STATUS_DEFAULT)
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |期待下次遇见你~
                """.trimMargin())
            }
            else -> {
                return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                        msgType = WXService.TYPE_TEXT, content = """
                    |请核对您的报名信息~
                    |
                    |学校：${data.school}
                    |
                    |年级：${data.grade}
                    |
                    |姓名：${data.name}
                    |
                    |性别：${data.sex}
                    |
                    |取向：${data.sexto}
                    |
                    |简介：${data.description}
                    |
                    |联系方式：${data.contact}
                    |
                    |回复[确定] 回复[取消]
                """.trimMargin())
            }
        }
    }


    fun handleError(wxMessage: WXMessage): WXMessage {
        redisService.setStatus(wxMessage.fromUserName!!, STATUS_DEFAULT)
        return WXMessage(toUsername = wxMessage.fromUserName, fromUserName = wxMessage.toUsername, createTime = emojiFactory.getTimestamp(),
                msgType = WXService.TYPE_TEXT, content = """
                    |航概君不小心把数据弄丢了(ಥ_ಥ)
                    |
                    |请返回重试~
                """.trimMargin())
    }


}


@Service
@Transactional
class Act2CPMission(@Autowired val redisService: RedisService,
                    @Autowired val a1Repository: A1Repository,
                    @Autowired val emojiFactory: EmojiFactory) : Act {

    override val name: String
        get() = "A2"
    override val actName: String
        get() = "CP任务"
    override val entry: List<String>
        get() = listOf("任务")
    override val description: String
        get() = "一周CP挑战任务"
    override val timeStart: Date
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-01 00:00:00")
    override val timeEnd: Date
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-08-08 00:00:00")

    enum class Status(val value: String) {
        DEFAULT("ACT2_DEFAULT"),
        HINT("ACT2_HINT"),
        FILL_SCHOOL("ACT2_FILL_SCHOOL"),
        FILL_GRADE("ACT2_FILL_GRADE"),
        FILL_NAME("ACT2_FILL_NAME"),
        FILL_SEX("ACT2_FILL_SEX"),
        FILL_SEXTO("ACT2_FILL_SEXTO"),
        FILL_DESCRIPTION("ACT2_FILL_DESCRIPTION"),
        FILL_CONTACT("ACT2_FILL_CONTACT"),
        FILL_CONFIRM("ACT2_FILL_CONFIRM");

        companion object {
            private val map = Status.values().associateBy(Status::value)
            fun fromString(value: String) = map[value]
            fun hasStatus(value: String): Boolean = map.contains(value)
        }
    }

    override fun hasStatus(value: String): Boolean = Status.hasStatus(value)


    override fun handle(wxMessage: WXMessage, statusValue: String): WXMessage {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun checkSignAndMatch(wxMessage: WXMessage): Boolean {

        return true
    }
}
