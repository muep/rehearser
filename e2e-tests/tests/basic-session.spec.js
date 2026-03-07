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

  test("Create new tune from empty search result", async ({ page }) => {
    await page.goto("/tunes.html");

    // Add a few tunes using the helper function
    await addTune(page, "Within a mile of Dublin", "A reel");
    await addTune(page, "Did You Wash Your Father’s Shirt", "A reel");

    // Verify the tunes appear in the list
    await expect(page.locator("body")).toContainText("Within a mile of Dublin");
    await expect(page.locator("body")).toContainText(
      "Did You Wash Your Father’s Shirt",
    );

    // Navigate to rehearsals page
    await page.goto("/rehearsals.html");

    // Start a new rehearsal
    await page.fill("input[name='title']", "Hasty practice session");
    await page.click("button[type='submit']:has-text('Start new')");

    // Navigate to the rehearsal page
    await page.click("a:has-text('Hasty practice session')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);

    // Navigate to the add entry page
    await page.click("a:has-text('Add entry')");
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry-add-search\.html/);

    // Search for a tune that hasn't been added
    await page.fill("input[name='query']", "Coffee jig");
    await page.click("input[type='submit']");

    // Check for the "No results found for Coffee jig" text
    await expect(page.locator(".search-no-results")).toContainText(
      "No results found for Coffee jig",
    );

    // Check for presence of a link to add the tune
    await expect(page.locator("body")).toContainText("Create new tune");
    await expect(page.locator("body")).toContainText('Create "Coffee jig"');

    // Follow the link to new tune page
    await page.click('text=Create "Coffee jig"');
    await expect(page).toHaveURL(
      /\/tunes\/new-tune\.html\?title=Coffee\+jig(&|$)/,
    );

    // Verify the tune title is pre-filled with the search term
    await expect(page.locator("input[name='title']")).toHaveValue("Coffee jig");

    // Save the new tune
    await page.fill("textarea[name='description']", "A lively jig");
    await page.click("button[type='submit']:has-text('Save')");

    // Verify we're redirected back to the entry search page
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/entry-add-search\.html/);

    // Search for the newly created tune
    await page.fill("input[name='query']", "Coffee jig");
    await page.click("input[type='submit']");

    // Verify the tune now appears in search results
    await expect(page.locator("body")).toContainText("Coffee jig");

    // Click the tune to add it as an entry
    await page.click('a:has-text("Coffee jig")');
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/new-entry\.html/);

    // Fill out entry details and save
    await page.fill("textarea[name='remarks']", "Practiced the B part");
    await page.click("input[type='submit']");

    // Verify we're back at the rehearsal page with the new entry
    await expect(page).toHaveURL(/\/rehearsals\/\d+\/rehearsal\.html/);
    await expect(page.locator("body")).toContainText("Coffee jig");
  });
});
