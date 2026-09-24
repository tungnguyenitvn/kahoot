import { test, expect, Browser, Page } from '@playwright/test';

// The release checklist's browser check, as a script: deep link through nginx, WebSocket live badge, guest join
// by PIN, host start, guest answer, reveal, leaderboard, finish, archived history. Runs against the demo seed of
// the release stack: the demo quiz "Java & Angular" has three questions whose correct options are A, B, C.
const HOST_USER = process.env['E2E_HOST_USER'] ?? 'host@example.test';
const HOST_PASSWORD = process.env['E2E_HOST_PASSWORD'] ?? 'smoke-only';
const GUEST_NAME = 'Ann ' + Date.now().toString(36);

async function hostOpensRoom(browser: Browser): Promise<{ host: Page; roomId: string; pin: string }> {
  const host = await (await browser.newContext()).newPage();
  await host.goto('/login');
  await host.getByRole('textbox', { name: 'Email' }).fill(HOST_USER);
  await host.getByRole('textbox', { name: 'Mật khẩu' }).fill(HOST_PASSWORD);
  await host.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(host).toHaveURL(/\/host$/);
  await host.getByRole('button', { name: 'Mở phòng →' }).first().click();
  await expect(host).toHaveURL(/\/room\/[0-9a-f-]{36}$/);
  const roomId = host.url().split('/room/')[1];
  const pin = (await host.locator('.pin').textContent())!.trim();
  expect(pin).toMatch(/^\d{6}$/);
  return { host, roomId, pin };
}

test('published stack: a full round through nginx, WebSocket, REST and the archive', async ({ browser }) => {
  const { host, roomId, pin } = await hostOpensRoom(browser);

  // A reload on the deep link still loads the bundle and the room (ROOM-04), and the WebSocket badge goes live through nginx.
  await host.goto('/room/' + roomId);
  await expect(host.locator('.pin')).toHaveText(pin);
  await expect(host.locator('.badge', { hasText: 'Trực tiếp' })).toBeVisible();

  const guest = await (await browser.newContext()).newPage();
  await guest.goto('/');
  await guest.getByRole('textbox', { name: 'PIN phòng' }).fill(pin);
  await guest.getByRole('textbox', { name: 'Tên hiển thị' }).fill(GUEST_NAME);
  await guest.getByRole('button', { name: 'Tham gia →' }).click();
  await expect(guest).toHaveURL(new RegExp('/room/' + roomId + '$'));
  await expect(guest.locator('.badge', { hasText: 'Trực tiếp' })).toBeVisible();
  await expect(guest.getByText('Chờ người dẫn bắt đầu…')).toBeVisible();

  // The host sees the joined player over WebSocket and starts the first question.
  await expect(host.locator('.player-list').getByText(GUEST_NAME)).toBeVisible();
  await host.getByRole('button', { name: 'Bắt đầu →' }).click();
  await expect(guest.locator('button.answer')).toHaveCount(4);

  // The guest answers A (correct for question 1) and gets the receipt in the UI.
  await guest.locator('button.answer').nth(0).click();
  await guest.getByRole('button', { name: 'Gửi đáp án' }).click();
  await expect(guest.getByText('Đã ghi nhận đáp án A.')).toBeVisible();

  // Reveal publishes the score: the leaderboard shows 1000 for the guest on both screens.
  await host.getByRole('button', { name: 'Chốt câu & công bố' }).click();
  await expect(guest.locator('.leaderboard .rank', { hasText: GUEST_NAME })).toContainText('1000');
  await expect(host.locator('.leaderboard .rank', { hasText: GUEST_NAME })).toContainText('1000');

  // The host walks the remaining questions to FINISHED without further answers.
  for (let round = 0; round < 8; round++) {
    const next = host.getByRole('button', { name: /Câu tiếp theo →|Kết thúc/ });
    const reveal = host.getByRole('button', { name: 'Chốt câu & công bố' });
    if (await host.getByText('HOÀN THÀNH').isVisible()) break;
    if (await next.isVisible()) await next.click();
    else if (await reveal.isVisible()) await reveal.click();
    await host.waitForTimeout(300);
  }
  await expect(host.getByText('HOÀN THÀNH')).toBeVisible();
  await expect(guest.getByText('HOÀN THÀNH')).toBeVisible();

  // The archive commits after FINISHED; the host's history returns the score through the same proxy.
  await expect.poll(async () => {
    const response = await host.request.get('/api/history/' + roomId);
    if (response.status() !== 200) return response.status();
    const rows = (await response.json()) as { name: string; score: number }[];
    return rows.find(row => row.name === GUEST_NAME)?.score ?? -1;
  }, { timeout: 30_000, intervals: [1000] }).toBe(1000);
});
