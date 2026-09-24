import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { Auth } from '../../core/auth';
import { Login } from './login.page';

describe('login page', () => {
  it('LOGIN-04 a failed login shows the error, stays on the route, keeps the username and clears only the password', async () => {
    const navigated: string[] = [];
    TestBed.configureTestingModule({ providers: [
      { provide: Auth, useValue: { login: async () => { throw new HttpErrorResponse({ status: 401, error: { code: 'INVALID_CREDENTIALS' } }); } } },
      { provide: Router, useValue: { navigateByUrl: async (url: string) => { navigated.push(url); return true; } } },
    ] });
    const fixture = TestBed.createComponent(Login);
    const page = fixture.componentInstance;
    page.model.set({ username: 'host@example.test', password: 'wrong' });
    fixture.detectChanges();
    expect(page.fields().invalid()).toBe(false);

    await page.login(new Event('submit'));
    fixture.detectChanges();

    expect(navigated).toEqual([]);
    expect(page.model()).toEqual({ username: 'host@example.test', password: '' });
    expect(page.busy()).toBe(false);
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('INVALID_CREDENTIALS');
    expect((fixture.nativeElement.querySelector('input[type=email]') as HTMLInputElement).value).toBe('host@example.test');
  });
});
