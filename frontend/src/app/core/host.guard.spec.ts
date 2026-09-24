import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { describe, expect, it } from 'vitest';
import { routes } from '../app.routes';
import { Auth } from './auth';
import { Login } from '../features/login/login.page';
import { Studio } from '../features/studio/studio.page';
import { Identity } from '../shared/models/identity';

function withUser(user: Identity) {
  TestBed.configureTestingModule({ providers: [
    provideRouter(routes), provideLocationMocks(), provideHttpClient(), provideHttpClientTesting(),
    { provide: Auth, useValue: { ready: async () => {}, user: signal<Identity | null>(user) } },
  ] });
}

describe('host route guard', () => {
  it('LOGIN-02 STUDIO-01 a guest asking for /host lands on /login and Studio never renders', async () => {
    withUser({ id: 'G:1', name: 'Guest', host: false });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/host');
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(component).toBeInstanceOf(Login);
  });

  it('a host reaches /host', async () => {
    withUser({ id: 'U:1', name: 'Host', host: true });
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/host');
    expect(TestBed.inject(Router).url).toBe('/host');
    expect(component).toBeInstanceOf(Studio);
  });
});
