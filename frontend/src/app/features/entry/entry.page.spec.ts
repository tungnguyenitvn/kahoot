import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { Api } from '../../core/http';
import { Auth } from '../../core/auth';
import { Entry } from './entry.page';

describe('join room page', () => {
  it('JOIN-01 a PIN that is not six digits keeps submit disabled and never calls the API; a valid form posts once', async () => {
    const posts: string[] = [];
    TestBed.configureTestingModule({ providers: [
      { provide: Api, useValue: { post: async (url: string) => { posts.push(url); return { id: 'room-1' }; }, get: async () => ({}) } },
      { provide: Auth, useValue: { ready: async () => {} } },
      { provide: Router, useValue: { navigate: async () => true } },
    ] });
    const fixture = TestBed.createComponent(Entry);
    const page = fixture.componentInstance;

    page.model.set({ pin: '12', name: 'Ann' });
    fixture.detectChanges();
    expect(page.fields().invalid()).toBe(true);
    expect((fixture.nativeElement.querySelector('button.primary') as HTMLButtonElement).disabled).toBe(true);
    await page.join(new Event('submit'));
    expect(posts).toEqual([]);

    page.model.set({ pin: '123456', name: 'Ann' });
    fixture.detectChanges();
    expect(page.fields().invalid()).toBe(false);
    await page.join(new Event('submit'));
    expect(posts).toEqual(['/api/rooms/join']);
  });
});
