const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_fit_123";
const BOB_UID = "bob_fit_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

function validProfile(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    displayName: "Alice Runner",
    age: 28,
    fitnessGoal: "Endurance",
    preferredIntensity: "Moderate",
    preferredCategories: ["Running", "Yoga"],
    dailyMinutesGoal: 30,
    dailyCaloriesGoal: 300,
    weeklyWorkoutsGoal: 5,
    themePreference: "system",
    privacyAnalyticsOptOut: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validWorkout(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    title: "Morning Run",
    category: "Running",
    durationSeconds: 1800,
    caloriesBurned: 260,
    intensity: "Moderate",
    dateKey: "2026-11-13",
    notes: "Felt energized",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validGoal(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    title: "30m Active Time",
    targetValue: 30,
    currentValue: 15,
    unit: "min",
    dateKey: "2026-11-13",
    completed: false,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validExercise(uid) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    name: "Goblet Squat",
    category: "Gym",
    instructions: "Hold weight at chest, squat with straight back, drive through heels.",
    difficulty: "Beginner",
    caloriesPerMinute: 7,
    defaultRestSeconds: 60,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workouts")
      .doc("w1")
      .set(validWorkout(ALICE_UID))
  );
});

test("Owner can create and read their own profile; cross-user read fails; list users fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Shadow update with ghost field on UserProfile fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Owner can CRUD workouts, daily_goals, and custom_exercises", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const workoutsRef = aliceDb.collection("users").doc(ALICE_UID).collection("workouts");
  const goalsRef = aliceDb.collection("users").doc(ALICE_UID).collection("daily_goals");
  const exercisesRef = aliceDb.collection("users").doc(ALICE_UID).collection("custom_exercises");

  await assertSucceeds(workoutsRef.doc("w1").set(validWorkout(ALICE_UID)));
  await assertSucceeds(workoutsRef.get());
  await assertSucceeds(
    workoutsRef.doc("w1").update({ caloriesBurned: 300, updatedAt: pastTimestamp() })
  );
  await assertSucceeds(workoutsRef.doc("w1").delete());

  await assertSucceeds(goalsRef.doc("g1").set(validGoal(ALICE_UID)));
  await assertSucceeds(goalsRef.get());
  await assertSucceeds(
    goalsRef.doc("g1").update({ currentValue: 30, completed: true, updatedAt: pastTimestamp() })
  );

  await assertSucceeds(exercisesRef.doc("e1").set(validExercise(ALICE_UID)));
  await assertSucceeds(exercisesRef.get());
});

test("Cross-user access and UID spoofing fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w1").set(validWorkout(ALICE_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workouts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).collection("workouts").doc("w2").set(validWorkout(ALICE_UID))
  );
});

test("Invalid workout category or out-of-range duration fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badWorkout = { ...validWorkout(ALICE_UID), category: "Flying" };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_bad").set(badWorkout)
  );
  const zeroDuration = { ...validWorkout(ALICE_UID), durationSeconds: 0 };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("workouts").doc("w_zero").set(zeroDuration)
  );
});
