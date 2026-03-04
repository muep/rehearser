import { test, expect } from "@playwright/test";

// Helper function to generate random username
test.describe("Signup and Login Flow", () => {
  const randomSuffix = Math.floor(Math.random() * 10000);
  const testUsername = `testuser${randomSuffix}`;
  const testPassword = "testpassword123";

  test("should allow user to signup and then login", async ({ page }) => {
    // Navigate to signup page
    await page.goto("/signup.html");
    await expect(page).toHaveTitle(/Rehearser/);
    
    // Fill out signup form
    await page.fill("input[name='username']", testUsername);
    await page.fill("input[name='password']", testPassword);
    await page.click("button[type='submit']");
    
    // Should redirect to index page after successful signup (which shows login form)
    await expect(page).toHaveURL("/index.html");
    
    // Now login with the new credentials (already on index.html with login form)
    await page.fill("input[name='username']", testUsername);
    await page.fill("input[name='password']", testPassword);
    await page.click("button[type='submit']");
    
    // Should stay on index page after successful login but now show logged-in content
    await expect(page).toHaveURL("/index.html");
    
    // Verify we're logged in (check for logout button or user info)
    await expect(page.locator("body")).toContainText("Logged in");
  });
});