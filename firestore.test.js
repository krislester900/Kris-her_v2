const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CAROL_UID = "carol_789";
// chatId = UID triés, joints par « _ »
const CHAT_ID = [ALICE_UID, BOB_UID].sort().join("_");

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules, host: emulatorHost, port: emulatorPort },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

// Le contexte de @firebase/rules-unit-testing renvoie un Firestore « compat » : on utilise donc FieldValue compat.
const firebase = require("firebase/compat/app");
require("firebase/compat/firestore");
const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();

async function seedChat() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("chats").doc(CHAT_ID).set({
      id: CHAT_ID,
      members: [ALICE_UID, BOB_UID].sort(),
      createdBy: ALICE_UID,
      createdAt: new Date(),
    });
  });
}

function message(id, senderId, extra = {}) {
  return {
    id,
    conversationId: CHAT_ID,
    senderId,
    senderName: "Alice",
    content: "Hello",
    type: "TEXT",
    status: "SENT",
    createdAt: serverTimestamp(),
    ...extra,
  };
}

// --- Profils ---

test("Unauthenticated user: cannot read profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Alice can create and read her own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      avatarColorHex: "#000000",
      statusText: "En ligne",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Bob cannot write Alice's profile", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacked",
      createdAt: new Date(),
    })
  );
});

// --- Discussions partagées ---

test("Alice can create a chat with Bob (sorted ids, deterministic chatId)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_ID).set({
      id: CHAT_ID,
      members: [ALICE_UID, BOB_UID].sort(),
      createdBy: ALICE_UID,
      createdAt: serverTimestamp(),
    })
  );
});

test("Chat creation fails when the chatId does not match the members", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("chats").doc("random_id").set({
      id: "random_id",
      members: [ALICE_UID, BOB_UID].sort(),
      createdBy: ALICE_UID,
      createdAt: serverTimestamp(),
    })
  );
});

test("Chat creation fails when the creator is not a member", async () => {
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();
  await assertFails(
    carolDb.collection("chats").doc(CHAT_ID).set({
      id: CHAT_ID,
      members: [ALICE_UID, BOB_UID].sort(),
      createdBy: CAROL_UID,
      createdAt: serverTimestamp(),
    })
  );
});

test("Both members can read the chat, a third user cannot", async () => {
  await seedChat();
  await assertSucceeds(testEnv.authenticatedContext(ALICE_UID).firestore().collection("chats").doc(CHAT_ID).get());
  await assertSucceeds(testEnv.authenticatedContext(BOB_UID).firestore().collection("chats").doc(CHAT_ID).get());
  await assertFails(testEnv.authenticatedContext(CAROL_UID).firestore().collection("chats").doc(CHAT_ID).get());
});

test("Listing chats: only through a members array-contains query", async () => {
  await seedChat();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("chats").where("members", "array-contains", BOB_UID).get());
  await assertFails(bobDb.collection("chats").get());
});

test("Alice sends a message and Bob can read it (real delivery)", async () => {
  await seedChat();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", ALICE_UID)));
  await assertSucceeds(bobDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").get());
  await assertSucceeds(bobDb.collection("chats").doc(CHAT_ID).collection("messages").get());
});

test("A third user cannot read or write messages", async () => {
  await seedChat();
  const carolDb = testEnv.authenticatedContext(CAROL_UID).firestore();
  await assertFails(carolDb.collection("chats").doc(CHAT_ID).collection("messages").get());
  await assertFails(carolDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", CAROL_UID)));
});

test("Cannot send a message pretending to be the other member", async () => {
  await seedChat();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", BOB_UID)));
});

test("Invalid message type is rejected", async () => {
  await seedChat();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", ALICE_UID, { type: "INVALID_UNKNOWN_TYPE" }))
  );
});

test("Only the recipient can mark a message as read, content is immutable", async () => {
  await seedChat();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", ALICE_UID)));

  // L'expéditeur ne peut pas se marquer « lu » lui-même
  await assertFails(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").update({ status: "READ", updatedAt: serverTimestamp() }));
  // Le destinataire peut
  await assertSucceeds(bobDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").update({ status: "READ", updatedAt: serverTimestamp() }));
  // Personne ne peut modifier le contenu
  await assertFails(bobDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").update({ content: "tampered", updatedAt: serverTimestamp() }));
});

test("Either member can react; only the author can delete a message", async () => {
  await seedChat();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").set(message("m1", ALICE_UID)));

  await assertSucceeds(bobDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").update({ reactionEmoji: "👍", updatedAt: serverTimestamp() }));
  await assertFails(bobDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").delete());
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_ID).collection("messages").doc("m1").delete());
});

// --- E-mails ---

test("Mail documents are never readable by clients", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("mail").doc("x").set({ to: ["alice@example.com"] });
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com" }).firestore();
  await assertFails(aliceDb.collection("mail").doc("x").get());
});

test("A user can only queue an e-mail addressed to herself", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com" }).firestore();
  await assertSucceeds(
    aliceDb.collection("mail").add({ to: ["alice@example.com"], message: { subject: "Bienvenue" }, createdAt: new Date() })
  );
  await assertFails(
    aliceDb.collection("mail").add({ to: ["someone.else@example.com"], message: { subject: "Spam" }, createdAt: new Date() })
  );
});

// --- Vie privée des numéros et données privées ---

test("A public profile cannot contain a plain phone number", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      phoneNumber: "+2250707070707",
      createdAt: serverTimestamp(),
    })
  );
});

test("A public profile with a phone hash and lowercase name is accepted", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      displayNameLower: "alice",
      phoneHash: "a".repeat(64),
      createdAt: serverTimestamp(),
    })
  );
});

test("Private contact and push token are readable only by their owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("contact").set({ phoneNumber: "+2250707070707" }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("push").set({ token: "tok", updatedAt: serverTimestamp() }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("contact").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("contact").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("push").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("push").set({ token: "evil" }));
});
