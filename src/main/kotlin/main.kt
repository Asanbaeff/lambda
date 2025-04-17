data class Message(
    val id: Int,
    val fromUserId: Int,
    val toUserId: Int,
    var text: String,
    var isRead: Boolean = false
)

data class Chat(
    val user1Id: Int,
    val user2Id: Int,
    val messages: MutableList<Message> = mutableListOf()
) {
    // Проверка, что чат содержит пользователя
    fun hasUser(userId: Int) = user1Id == userId || user2Id == userId

    // Получить ID собеседника для данного пользователя
    fun getCompanion(userId: Int): Int? =
        when (userId) {
            user1Id -> user2Id
            user2Id -> user1Id
            else -> null
        }
}

class ChatService {
    private val chats = mutableListOf<Chat>()
    private var messageAutoIncrement = 0

    // Создать чат (если не существует)
    private fun createChatIfNotExists(user1: Int, user2: Int): Chat {
        return chats.find { (it.user1Id == user1 && it.user2Id == user2) || (it.user1Id == user2 && it.user2Id == user1) }
            ?: Chat(user1, user2).also { chats.add(it) }
    }

    // Создать новое сообщение (если чат не существует - создаётся)
    fun createMessage(fromUserId: Int, toUserId: Int, text: String): Message {
        val chat = createChatIfNotExists(fromUserId, toUserId)
        val message = Message(++messageAutoIncrement, fromUserId, toUserId, text)
        chat.messages.add(message)
        return message
    }

    // Редактировать сообщение по id
    fun editMessage(messageId: Int, newText: String): Boolean {
        chats.forEach { chat ->
            chat.messages.find { it.id == messageId }?.let {
                it.text = newText
                return true
            }
        }
        return false
    }

    // Удалить сообщение по id (из любого чата)
    fun deleteMessage(messageId: Int): Boolean {
        chats.forEach { chat ->
            if (chat.messages.removeIf { it.id == messageId }) return true
        }
        return false
    }

    // Удалить чат с собеседником для пользователя (удаляет всю переписку)
    fun deleteChat(userId: Int, companionUserId: Int): Boolean {
        return chats.removeIf {
            (it.user1Id == userId && it.user2Id == companionUserId) ||
                    (it.user1Id == companionUserId && it.user2Id == userId)
        }
    }

    // Получить список всех чатов для пользователя (список собеседников)
    fun getChats(userId: Int): List<Int> =
        chats.filter { it.hasUser(userId) }
            .mapNotNull { it.getCompanion(userId) }

    // Получить количество чатов с непрочитанными сообщениями для пользователя
    fun getUnreadChatsCount(userId: Int): Int =
        chats.filter { it.hasUser(userId) }
            .filter { chat ->
                chat.messages.any { !it.isRead && it.toUserId == userId }
            }.count()

    // Получить список последних сообщений из всех чатов пользователя в виде строк.
    // Если сообщений нет - "нет сообщений"
    fun getLastMessages(userId: Int): List<String> =
        chats.filter { it.hasUser(userId) }
            .map { chat ->
                val lastMsg = chat.messages.lastOrNull()
                lastMsg?.let { "От ${it.fromUserId} к ${it.toUserId}: ${it.text}" } ?: "нет сообщений"
            }

    // Получить список последних N сообщений из чата с указанным собеседником.
    // После вызова все отданные сообщения считаются прочитанными.
    fun getMessagesFromChat(userId: Int, companionUserID: Int, countMessages: Int): List<Message> {
        val chat = chats.find {
            (it.user1Id == userId && it.user2Id == companionUserID) ||
                    (it.user1Id == companionUserID && it.user2Id == userId)
        } ?: return emptyList()

        val lastMessages = chat.messages.takeLast(countMessages)
        lastMessages.filter { !it.isRead && it.toUserId == userId }.forEach { it.isRead = true }
        return lastMessages
    }
}

fun main() {
    val service = ChatService()

    service.createMessage(1, 2, "Привет!")
    service.createMessage(2, 1, "Привет! Как дела?")
    service.createMessage(3, 1, "Здравствуй!")

    println("Чаты пользователя 1:")
    println(service.getChats(1)) // [2,3]

    println("Непрочитанные чаты у пользователя 1:")
    println(service.getUnreadChatsCount(1)) // Должно быть >0

    println("Последние сообщения у пользователя 1:")
    service.getLastMessages(1).forEach(::println)

    println("Сообщения из чата с пользователем 2:")
    service.getMessagesFromChat(1, 2, 10).forEach {
        println("${it.fromUserId} -> ${it.toUserId}: ${it.text} [прочитано=${it.isRead}]")
    }

    println("Непрочитанные чаты после чтения:")
    println(service.getUnreadChatsCount(1)) // Должно уменьшиться

}