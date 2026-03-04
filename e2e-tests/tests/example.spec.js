import { test, expect } from "@playwright/test";

test("basic test example", async ({ page }) => {
  await page.goto("/");

  // Example: Check if the page title is correct
  await expect(page).toHaveTitle(/Rehearser/); // Adjust the regex to match your app's title
});
