import { test, expect } from "@playwright/test";

const addTune = async (page, title, description) => {
  await page.click("a:has-text('Add new')");
  await expect(page).toHaveURL("/tunes/new-tune.html");
  await page.fill("input[name='title']", title);
  await page.fill("textarea[name='description']", description);
  await page.click("button[type='submit']:has-text('Save')");
  await expect(page).toHaveURL("/tunes.html");
};

const addEntry = async (page, query, remarks) => {
  await page.click("a:has-text('Add entry')");
  await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry-add-search\.html/);
  await page.fill("input[name='query']", query);
  await page.click("input[type='submit']");
  await page.locator("a", { hasText: query }).first().click();
  await expect(page).toHaveURL(/\/rehearsals\/\d+\/new-entry\.html/);
  await page.fill("textarea[name='remarks']", remarks);
  await page.click("input[type='submit']");
  await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);
};

const prepareUser = async ({ page }) => {
  // Signup and login once, before each test
  const testPassword = "testpassword123";
  const randomSuffix = Math.floor(Math.random() * 1000000);
  const timestampHash = Date.now().toString().slice(-4);
  const testUsername = `testuser${randomSuffix}${timestampHash}`;

  await page.goto("/signup.html");
  await page.fill("input[name='username']", testUsername);
  await page.fill("input[name='password']", testPassword);
  await page.click("button[type='submit']");

  await page.goto("/index.html");
  await page.fill("input[name='username']", testUsername);
  await page.fill("input[name='password']", testPassword);
  await page.click("button[type='submit']");
};

test.describe("Editing rehearsal entries", () => {
  test.beforeEach(prepareUser);

  test("Add entries and change one entry from one tune to another", async ({
    page,
  }) => {
    // Set up a couple of tunes to work with
    await page.goto("/tunes.html");
    await addTune(page, "Kesh jig", "A traditional Irish jig");
    await addTune(page, "Cooley's reel", "A popular session reel");

    // Start a new rehearsal
    await page.goto("/rehearsals.html");
    await page.fill("input[name='title']", "Entry editing session");
    await page.click("button[type='submit']:has-text('Start new')");
    await expect(page).toHaveURL("/rehearsals.html");

    // Open the rehearsal detail page, which hosts the "Add entry" link
    await page.click("a:has-text('Entry editing session')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);

    // Add two entries to the rehearsal
    await addEntry(page, "Kesh jig", "Worked on ornamentation");
    await addEntry(page, "Cooley's reel", "Focused on bowing patterns");

    // Both entries are listed on the rehearsal page
    await expect(page.locator("body")).toContainText("Kesh jig");
    await expect(page.locator("body")).toContainText("Cooley's reel");

    // Open the first entry from the entries list
    await page.click("a:has-text('Kesh jig')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry\/\d+\/entry\.html/);

    // The edit form is pre-filled with the entry's tune and remarks
    await expect(page.locator("#exercise-id")).toHaveValue(
      await page
        .locator("#exercise-id option", { hasText: "Kesh jig" })
        .getAttribute("value"),
    );
    await expect(page.locator("textarea[name='remarks']")).toHaveValue(
      "Worked on ornamentation",
    );

    // Change the entry's tune from Kesh jig to Cooley's reel and update notes
    await page.selectOption("#exercise-id", { label: "Cooley's reel" });
    await page.fill(
      "textarea[name='remarks']",
      "Switched this slot to work on bowing",
    );
    await page.click("input[type='submit']");

    // The form posts back to the same entry page
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry\/\d+\/entry\.html/);

    // The edited entry now points at the other tune
    await expect(page.locator("#exercise-id")).toHaveValue(
      await page
        .locator("#exercise-id option", { hasText: "Cooley's reel" })
        .getAttribute("value"),
    );
    await expect(page.locator("body")).toContainText("Practiced");

    // On the rehearsal page, the entry now appears under its new tune
    await page.goto("/rehearsals.html");
    await page.click("a:has-text('Entry editing session')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);
    const entries = page.locator("main ul li");
    await expect(entries).toHaveCount(2);
    await expect(entries.filter({ hasText: "Cooley's reel" })).toHaveCount(2);
    await expect(entries.filter({ hasText: "Kesh jig" })).toHaveCount(0);

    // The edited entry's detail page shows the updated remarks
    await entries
      .filter({ hasText: "Cooley's reel" })
      .first()
      .locator("a")
      .click();
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry\/\d+\/entry\.html/);
    await expect(page.locator("textarea[name='remarks']")).toHaveValue(
      "Switched this slot to work on bowing",
    );
  });
});
