import { Pipe, PipeTransform } from '@angular/core';
import { StatusHistoryEntry } from '../models/order.model';
import { isSagaLive } from '../models/saga';

@Pipe({ name: 'sagaLive', standalone: true, pure: true })
export class SagaLivePipe implements PipeTransform {
  transform(history: StatusHistoryEntry[] | null | undefined): boolean {
    return isSagaLive(history);
  }
}
