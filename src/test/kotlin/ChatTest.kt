import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChatServiceTest {

    private lateinit var service: ChatService

    @Before
    fun setup() {
        service = ChatService()
    }

    @Test
    fun testCreateMessage_createsChatIfNotExists() {
        val msg = service.createMessage(1, 2, "Hello")
        assertEquals(1, msg.id)
        assertEquals(1, msg.fromUserId)
        assertEquals(2, msg.toUserId)
        assertEquals("Hello", msg.text)
        assertFalse(msg.isRead)
        val chats = service.getChats(1)
        assertTrue(chats.contains(2))
    }

    @Test
    fun testEditMessage_existingMessage_returnsTrueAndEdits() {
        val msg = service.createMessage(1, 2, "Hello")
        val result = service.editMessage(msg.id, "Hi")
        assertTrue(result)
        val messages = service.getMessagesFromChat(1, 2, 10)
        val editedMsg = messages.find { it.id == msg.id }
        assertNotNull(editedMsg)
        assertEquals("Hi", editedMsg?.text)
    }

    @Test
    fun testEditMessage_nonExistingMessage_returnsFalse() {
        val result = service.editMessage(999, "New text")
        assertFalse(result)
    }

    @Test
    fun testDeleteMessage_existingMessage_returnsTrueAndRemoves() {
        val msg = service.createMessage(1, 2, "Hello")
        val deleted = service.deleteMessage(msg.id)
        assertTrue(deleted)
        val messages = service.getMessagesFromChat(1, 2, 10)
        assertTrue(messages.none { it.id == msg.id })
    }

    @Test
    fun testDeleteMessage_nonExistingMessage_returnsFalse() {
        val deleted = service.deleteMessage(999)
        assertFalse(deleted)
    }

    @Test
    fun testDeleteChat_existingChat_returnsTrueAndRemoves() {
        service.createMessage(1, 2, "Hello")
        val deleted = service.deleteChat(1, 2)
        assertTrue(deleted)
        val chatsUser1 = service.getChats(1)
        val chatsUser2 = service.getChats(2)
        assertFalse(chatsUser1.contains(2))
        assertFalse(chatsUser2.contains(1))
    }

    @Test
    fun testDeleteChat_nonExistingChat_returnsFalse() {
        val deleted = service.deleteChat(1, 3) // чат не создавался с таким пользователем
        assertFalse(deleted)
    }

    @Test
    fun testGetChats_returnsAllCompanionsForUser() {
        service.createMessage(1, 2, "Hi")
        service.createMessage(3, 1, "Hey")
        val chatsForUser1 = service.getChats(1).sorted()
        assertEquals(listOf(2, 3), chatsForUser1)
        val chatsForUser4 = service.getChats(4)
        assertTrue(chatsForUser4.isEmpty())
    }

    @Test
    fun testGetUnreadChatsCount_countsCorrectly() {

        service.createMessage(1, 2, "msg1") // непрочитано для 2
        val m2 = service.createMessage(3, 2, "msg2") // непрочитано для 2
        val m3 = service.createMessage(4, 2, "msg3")
        m3.isRead = true
        service.createMessage(5, 6, "msg4")
        val unreadCount = service.getUnreadChatsCount(2)
        assertEquals(2, unreadCount)
    }

    @Test
    fun testGetLastMessages_returnsLastMessagesOrNoMessagesString() {

        service.createMessage(1, 2, "first")
        Thread.sleep(10)
        service.createMessage(2, 1, "second")
        val chatWithNoMessagesCompanionId = 5
        val messageToRemove = service.createMessage(chatWithNoMessagesCompanionId, 6, "temp")
        service.deleteMessage(messageToRemove.id)
        val lastMessagesForUser1 = service.getLastMessages(1)
        assertEquals(listOf("От 2 к 1: second"), lastMessagesForUser1)
        val lastMessagesFor5 = service.getLastMessages(chatWithNoMessagesCompanionId)
        assertEquals(listOf("нет сообщений"), lastMessagesFor5)
    }

    @Test
    fun testGetMessagesFromChat_returnsLastNAndMarksAsRead() {

        val m1 = service.createMessage(1, 2, "msg1")
        Thread.sleep(10)
        val m2 = service.createMessage(2, 1, "msg2")
        Thread.sleep(10)
        val m3 = service.createMessage(1, 2, "msg3")
        val lastMsgsForUser2 = service.getMessagesFromChat(userId = 2, companionUserID = 1, countMessages = 2)
        assertEquals(listOf(m2, m1).map { it.id }, lastMsgsForUser2.map { it.id })
        lastMsgsForUser2.forEach { msg ->
            if (msg.toUserId == 2) {
                assertTrue(msg.isRead)
            }
        }
        val allMsgsInChatAfterCall =
            lastMsgsForUserToList(service.getChats(userId = 1).first(), service) ?: emptyList()
        allMsgsInChatAfterCall.find { it.id == m1.id }?.let {
            assertFalse(it.isRead)
        }
        val lastMsgsForUser1 = service.getMessagesFromChat(userId = 1, companionUserID = 2, countMessages = 3)

        lastMsgsForUser1.forEach { msg ->
            if (msg.toUserId == 1) {
                assertTrue(msg.isRead)
            }
        }
    }

    private fun lastMsgsForUserToList(companion: Int?, svc: ChatService): List<Message>? {
        if (companion == null) return null

        return svc.getMessagesFromChat(companion, companion, 1000)
    }
}