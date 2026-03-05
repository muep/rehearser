import { test, expect } from "@playwright/test";

// Helper function to add a tune
const addTune = async (page, title, description) => {
  await page.click("a:has-text('Add new')");
  await expect(page).toHaveURL("/tunes/new-tune.html");

  await page.fill("input[name='title']", title);
  await page.fill("textarea[name='description']", description);
  await page.click("button[type='submit']:has-text('Save')");

  // Should redirect back to tunes listing
  await expect(page).toHaveURL("/tunes.html");
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

test.describe("A simple session", () => {
  test.beforeEach(prepareUser);

  test("Insert some tunes and have a rehearsal", async ({ page }) => {
    await page.goto("/tunes.html");

    // Assuming already logged in
    await expect(page.locator("body")).toContainText("Logged in");

    // Add first tune using helper function
    await addTune(page, "Kesh jig", "A traditional Irish jig");

    // Verify the tune appears in the list
    await expect(page.locator("body")).toContainText("Kesh jig");

    // Add second tune using helper function
    await addTune(page, "Cooley's reel", "A popular session reel");

    // Add third tune using helper function
    await addTune(page, "Tell her I am", "A beautiful jig");

    // Verify all tunes are listed
    await expect(page.locator("body")).toContainText("Kesh jig");
    await expect(page.locator("body")).toContainText("Cooley's reel");
    await expect(page.locator("body")).toContainText("Tell her I am");

    // Navigate to rehearsals page
    await page.goto("/rehearsals.html");

    // Start a new rehearsal
    await page.fill("input[name='title']", "Morning practice session");
    await page.click("button[type='submit']:has-text('Start new')");

    // Should redirect back to rehearsals page
    await expect(page).toHaveURL("/rehearsals.html");

    // Verify the rehearsal appears as current
    await expect(page.locator("body")).toContainText(
      "Morning practice session",
    );
    await expect(page.locator("body")).toContainText("started at");

    // Click on the rehearsal to view details
    await page.click("a:has-text('Morning practice session')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);

    // Click "Add entry" link
    await page.click("a:has-text('Add entry')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry-add-search\.html/);

    // Search for Kesh jig
    await page.fill("input[name='query']", "Kesh");
    await page.click("input[type='submit']");

    // Add Kesh jig as first entry
    await page.click("a:has-text('Kesh jig')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/new-entry\.html/);

    // Fill out entry form (dropdown is already set correctly by server)
    await page.fill(
      "textarea[name='remarks']",
      "Worked on ornamentation and timing",
    );
    await page.click("input[type='submit']");

    // Should redirect back to rehearsal detail page
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);

    // Verify the entry appears
    await expect(page.locator("body")).toContainText("Kesh jig");

    // Go back to add another entry
    await page.click("a:has-text('Add entry')");

    // Search for Kesh jig
    await page.fill("input[name='query']", "cooley");
    await page.click("input[type='submit']");

    // Click on Cooley's reel from the recent tunes list
    await page.click('a:has-text("Cooley\'s reel")');

    // Fill out entry form for Cooley's reel (dropdown is already set correctly)
    await page.fill("textarea[name='remarks']", "Focused on bowing patterns");
    await page.click("input[type='submit']");

    // Verify both entries appear
    await expect(page.locator("body")).toContainText("Kesh jig");
    await expect(page.locator("body")).toContainText("Cooley's reel");

    // End the rehearsal
    await page.click("button[type='submit']:has-text('End rehearsal')");

    // Should stay on rehearsal detail page but now show duration
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);
    await expect(page.locator("body")).toContainText("Duration:");

    // Verify rehearsal appears in past rehearsals
    await page.goto("/rehearsals.html");
    await expect(page.locator("body")).toContainText(
      "Morning practice session",
    );
    await expect(page.locator("body")).toContainText("Past ones");
  });
});
