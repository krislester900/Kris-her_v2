package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun openChat_betweenTwoUsers_succeedsAndIsIdempotent() = runBlocking<Unit> {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    signInTestUser(ALICE_EMAIL)
    val repository = FirestoreChatRepository(firestore)

    val first = withTimeout(DEFAULT_TIMEOUT_MS) { repository.openChat(bobUid) }
    assertTrue(first.isSuccess)
    val chatId = first.getOrThrow()
    assertEquals(repository.chatIdFor(aliceUid, bobUid), chatId)

    // Ouvrir deux fois la même discussion renvoie le même identifiant
    val second = withTimeout(DEFAULT_TIMEOUT_MS) { repository.openChat(bobUid) }
    assertEquals(chatId, second.getOrThrow())
  }

  @Test
  fun sendChatMessage_aliceToBob_bobCanRead() = runBlocking<Unit> {
    signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreChatRepository(firestore)

    val chatId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.openChat(bobUid).getOrThrow() }
    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.sendChatMessage(chatId, "msg_1", "Hello Bob", "TEXT", 0L, null, "Alice")
    }
    assertTrue(sendResult.isSuccess)

    // Bob voit la discussion et le message d'Alice (vraie livraison entre deux comptes)
    signInTestUser(BOB_EMAIL)
    val bobRepo = FirestoreChatRepository(firestore)
    val chats = withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeChats().first() }
    assertTrue(chats.any { it.id == chatId })
    val snapshot = withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeChatMessages(chatId).first() }
    assertTrue(snapshot.messages.any { it.id == "msg_1" && it.content == "Hello Bob" })

    // Bob peut confirmer la lecture
    val readResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.markChatMessagesRead(chatId, listOf("msg_1")) }
    assertTrue(readResult.isSuccess)
  }

  @Test
  fun observeChats_unauthenticatedUser_emitsNothing() = runBlocking<Unit> {
    auth.signOut()
    val repository = FirestoreChatRepository(firestore)
    val first = withTimeoutOrNull(FLOW_TIMEOUT_MS) { repository.observeChats().firstOrNull() }
    assertTrue(first == null)
  }

  @Test
  fun crossUserAccess_thirdUserCannotReadChat() = runBlocking<Unit> {
    signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = FirestoreChatRepository(firestore)
    val chatId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.openChat(bobUid).getOrThrow() }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.sendChatMessage(chatId, "msg_secret", "Secret", "TEXT", 0L, null, "Alice").getOrThrow()
    }

    // Carol n'est pas membre : lecture refusée
    signInTestUser(CAROL_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("chats").document(chatId).collection("messages").document("msg_secret").get().await()
      }
      fail("Expected PERMISSION_DENIED for a non-member")
    } catch (exception: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception.code)
    }
  }

  @Test
  fun updateUserProfile_krisAndHer_storesInFirestore() = runBlocking<Unit> {
    // Kris sets custom profile
    signInTestUser(ALICE_EMAIL)
    val krisRepo = FirestoreChatRepository(firestore)
    val krisUid = krisRepo.requireUserId()

    val krisResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      krisRepo.updateUserProfile(
        displayName = "Kris",
        photoUrl = "preset:kris_noir",
        statusText = "Kris • En ligne • kris&her E2EE",
        avatarColorHex = "#FFFFFF"
      )
    }
    assertTrue("Kris profile update should succeed", krisResult.isSuccess)

    // Verify stored document in Firestore
    val krisDoc = firestore.collection("users").document(krisUid).get().await()
    assertTrue(krisDoc.exists())
    assertEquals("Kris", krisDoc.getString("displayName"))
    assertEquals("preset:kris_noir", krisDoc.getString("photoUrl"))
    assertEquals("Kris • En ligne • kris&her E2EE", krisDoc.getString("statusText"))

    // Her sets custom profile
    signInTestUser(BOB_EMAIL)
    val herRepo = FirestoreChatRepository(firestore)
    val herUid = herRepo.requireUserId()

    val herResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      herRepo.updateUserProfile(
        displayName = "Her",
        photoUrl = "preset:her_cyber",
        statusText = "Her • En ligne • kris&her E2EE",
        avatarColorHex = "#FF1493"
      )
    }
    assertTrue("Her profile update should succeed", herResult.isSuccess)

    val herDoc = firestore.collection("users").document(herUid).get().await()
    assertTrue(herDoc.exists())
    assertEquals("Her", herDoc.getString("displayName"))
    assertEquals("preset:her_cyber", herDoc.getString("photoUrl"))
    assertEquals("Her • En ligne • kris&her E2EE", herDoc.getString("statusText"))
  }

  @Test
  fun sendStickerMessage_succeeds() = runBlocking<Unit> {
    signInTestUser(ALICE_EMAIL)
    val bobUid = signInTestUser(BOB_EMAIL)
    signInTestUser(ALICE_EMAIL)
    val repository = FirestoreChatRepository(firestore)

    val chatId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.openChat(bobUid).getOrThrow() }
    val stickerResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendChatMessage(
        chatId = chatId,
        messageId = "stk_msg_1",
        content = "KRIS_LOVE",
        type = "STICKER",
        audioDurationSec = 0L,
        mediaUri = "preset://❤️",
        senderName = "Alice"
      )
    }
    assertTrue("Sticker message should send successfully", stickerResult.isSuccess)
  }

  @Test
  fun createAndObserveCustomSticker_succeeds() = runBlocking<Unit> {
    signInTestUser(ALICE_EMAIL)
    val repository = FirestoreChatRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createCustomSticker(
        title = "GALLERY_ZAP",
        imageUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        emojiBadge = "⚡",
        borderColorHex = "#FFD700",
        isVideo = false
      )
    }
    assertTrue("Custom sticker creation should succeed", createResult.isSuccess)
    val stickerId = createResult.getOrThrow()
    assertTrue(stickerId.startsWith("stk_"))

    val stickers = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeCustomStickers().first()
    }
    assertTrue("Should observe at least one custom sticker", stickers.isNotEmpty())
    val found = stickers.find { it.id == stickerId }
    assertTrue("Created sticker should exist in observed list", found != null)
    assertEquals("GALLERY_ZAP", found?.title)
    assertEquals("⚡", found?.emojiBadge)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@krisandher.test"
    const val BOB_EMAIL = "bob@krisandher.test"
    const val CAROL_EMAIL = "carol@krisandher.test"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
