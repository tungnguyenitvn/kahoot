import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
/** The HTTP boundary: relative URLs only, so Angular attaches the XSRF header; no retry of mutations here. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  get<T>(url: string) { return firstValueFrom(this.http.get<T>(url)); }
  post<T>(url: string, body: unknown) { return firstValueFrom(this.http.post<T>(url, body)); }
}
