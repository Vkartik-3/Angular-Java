import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);

  getMode(): Observable<boolean> {
    return this.http.get<boolean>('/api/payment/mode');
  }

  setMode(accept: boolean): Observable<string> {
    return this.http.put(`/api/payment/mode?accept=${accept}`, null, {
      responseType: 'text'
    });
  }

  getDelay(): Observable<number> {
    return this.http.get<number>('/api/payment/delay');
  }

  setDelay(seconds: number): Observable<string> {
    return this.http.put(`/api/payment/delay?seconds=${seconds}`, null, {
      responseType: 'text'
    });
  }

  getCrash(): Observable<boolean> {
    return this.http.get<boolean>('/api/payment/crash');
  }

  setCrash(enabled: boolean): Observable<string> {
    return this.http.put(`/api/payment/crash?enabled=${enabled}`, null, {
      responseType: 'text'
    });
  }
}