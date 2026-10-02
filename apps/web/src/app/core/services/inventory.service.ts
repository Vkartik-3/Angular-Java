import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private http = inject(HttpClient);

  getMode(): Observable<boolean> {
    return this.http.get<boolean>('/api/inventory/mode');
  }

  setMode(accept: boolean): Observable<string> {
    return this.http.put(`/api/inventory/mode?accept=${accept}`, null, {
      responseType: 'text'
    });
  }

  getDelay(): Observable<number> {
    return this.http.get<number>('/api/inventory/delay');
  }

  setDelay(seconds: number): Observable<string> {
    return this.http.put(`/api/inventory/delay?seconds=${seconds}`, null, {
      responseType: 'text'
    });
  }

  getCrash(): Observable<boolean> {
    return this.http.get<boolean>('/api/inventory/crash');
  }

  setCrash(enabled: boolean): Observable<string> {
    return this.http.put(`/api/inventory/crash?enabled=${enabled}`, null, {
      responseType: 'text'
    });
  }
}