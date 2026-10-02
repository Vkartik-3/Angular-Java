import { expect, Page, test } from '@playwright/test';

function requiredCredential(name: string): string {
  const value = process.env[name];
  if (!value) throw new Error(`Set ${name} in the environment before running E2E tests.`);
  return value;
}
const adminUsername = requiredCredential('E2E_ADMIN_USERNAME');
const adminPassword = requiredCredential('E2E_ADMIN_PASSWORD');
const userUsername = requiredCredential('E2E_USER_USERNAME');
const userPassword = requiredCredential('E2E_USER_PASSWORD');


async function login(page: Page, username: string, password: string) {
  await page.goto('/orders');
  await page.waitForURL(/localhost:8180/);
  await page.fill('#username', username);
  await page.fill('#password', password);
  await page.click('#kc-login');
  await page.waitForURL('**/orders', { timeout: 10_000 });
}

// Use Angular router links — avoids full page reloads which Keycloak would
// redirect back to /orders via the hardcoded redirectUri.
async function navigateToOrders(page: Page) {
  await page.locator('.btn-orders').click();
  await page.waitForURL('**/orders');
}

async function navigateToAdmin(page: Page) {
  await page.getByRole('button', { name: 'Admin' }).click();
  await page.waitForURL('**/admin');
}

test.describe('ShopFlow E2E', () => {

  // Guard against dirty state left by a previous run.
  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    await login(page, adminUsername, adminPassword);
    await navigateToAdmin(page);

    // Reset inventory acceptance mode if rejecting
    const acceptanceCard = page.locator('.card').filter({ hasText: 'Acceptance modes' });
    const rejectingBtn = acceptanceCard.getByRole('button', { name: 'Rejecting Orders' });
    if (await rejectingBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await rejectingBtn.click();
      await expect(acceptanceCard.getByRole('button', { name: 'Accept Orders' })).toBeVisible({ timeout: 3_000 });
    }

    // Reset crash modes if enabled
    const crashCard = page.locator('.card').filter({ hasText: 'Failure simulations' });
    const inventoryItem = crashCard.locator('.crash-item').filter({ hasText: 'Inventory consumer' });
    if (await inventoryItem.locator('.crash-badge').isVisible({ timeout: 2_000 }).catch(() => false)) {
      await inventoryItem.getByRole('button', { name: 'OFF' }).click();
    }
    const paymentItem = crashCard.locator('.crash-item').filter({ hasText: 'Payment consumer' });
    if (await paymentItem.locator('.crash-badge').isVisible({ timeout: 2_000 }).catch(() => false)) {
      await paymentItem.getByRole('button', { name: 'OFF' }).click();
    }

    await page.close();
  });

  test('1 - happy path: saga completes with Inventory Reserved', async ({ page }) => {
    await login(page, userUsername, userPassword);

    await page.getByRole('button', { name: 'New Order' }).click();
    await page.waitForURL('**/orders/new');

    await page.getByRole('button', { name: 'Add to Cart' }).first().click();
    await page.getByRole('button', { name: 'Place Order' }).click();
    await page.waitForURL(/\/orders\/[0-9a-f-]{36}/);

    // Saga timeline builds via SSE — allow up to 2× OutboxPublisherJob cycles (5s each)
    await expect(page.locator('.tl-label', { hasText: 'Order Created' })).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.tl-label', { hasText: 'Payment Confirmed' })).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('.tl-label', { hasText: 'Inventory Reserved' })).toBeVisible({ timeout: 30_000 });

    // Order appears in the list with correct final status
    await navigateToOrders(page);
    await expect(
      page.locator('p-tag').filter({ hasText: 'Inventory Reserved' }).first()
    ).toBeVisible({ timeout: 10_000 });
  });

  test('2 - failure path: inventory rejection triggers payment compensation', async ({ page }) => {
    await login(page, adminUsername, adminPassword);

    // Enable inventory rejection — use navbar link to avoid Keycloak redirect on full reload
    await navigateToAdmin(page);
    const acceptanceCard = page.locator('.card').filter({ hasText: 'Acceptance modes' });
    await acceptanceCard.getByRole('button', { name: 'Accept Orders' }).click();
    await expect(acceptanceCard.getByRole('button', { name: 'Rejecting Orders' })).toBeVisible({ timeout: 3_000 });

    // Place an order
    await navigateToOrders(page);
    await page.getByRole('button', { name: 'New Order' }).click();
    await page.waitForURL('**/orders/new');
    await page.getByRole('button', { name: 'Add to Cart' }).first().click();
    await page.getByRole('button', { name: 'Place Order' }).click();
    await page.waitForURL(/\/orders\/[0-9a-f-]{36}/);

    // Compensation saga timeline — 3 outbox cycles: payment-request, inventory-request, payment-rollback
    await expect(page.locator('.tl-label', { hasText: 'Order Created' })).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('.tl-label', { hasText: 'Payment Confirmed' })).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('.tl-label', { hasText: 'Inventory Rejected' })).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('.tl-label', { hasText: 'Payment Rolled Back' })).toBeVisible({ timeout: 45_000 });
    await expect(page.locator('p-tag').first()).toContainText('Inventory Rejected');

    // Reset: re-enable inventory acceptance
    await navigateToAdmin(page);
    await acceptanceCard.getByRole('button', { name: 'Rejecting Orders' }).click();
    await expect(acceptanceCard.getByRole('button', { name: 'Accept Orders' })).toBeVisible({ timeout: 3_000 });
  });

  test('3 - unauthenticated: /orders redirects to Keycloak login', async ({ page }) => {
    await page.goto('/orders');
    await expect(page).toHaveURL(/localhost:8180/, { timeout: 5_000 });
  });
});