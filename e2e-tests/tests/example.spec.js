import { test, expect } from "@playwright/test";

test("basic test example", async ({ page }) => {
  // Replace with your actual server URL and port
  await page.goto("http://localhost:3000");

  // Example: Check if the page title is correct
  await expect(page).toHaveTitle(/Rehearser/); // Adjust the regex to match your app's title
});
