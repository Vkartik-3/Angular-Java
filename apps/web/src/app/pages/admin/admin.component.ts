import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToggleButtonModule } from 'primeng/togglebutton';
import { Select } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { InventoryService } from '../../core/services/inventory.service';
import { PaymentService } from '../../core/services/payment.service';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { ImportResult } from '../../core/models/product.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ToggleButtonModule, Select, ButtonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminComponent implements OnInit {
  private inventoryService = inject(InventoryService);
  private paymentService = inject(PaymentService);
  private messageService = inject(MessageService);
  private productService = inject(ProductService);
  private cartService = inject(CartService);

  inventoryAccept = signal(true);
  paymentAccept = signal(true);
  paymentDelay = signal(0);
  inventoryDelay = signal(0);
  paymentCrash = signal(false);
  inventoryCrash = signal(false);

  selectedFile = signal<File | null>(null);
  importing = signal(false);
  importResult = signal<ImportResult | null>(null);
  importError = signal<string | null>(null);
  formatGuideOpen = signal(false);
  showSkipped = signal(false);

  readonly delayOptions = [
    { label: '0s', value: 0 },
    { label: '2s', value: 2 },
    { label: '4s', value: 4 },
    { label: '6s', value: 6 },
    { label: '8s', value: 8 },
  ];

  ngOnInit() {
    this.inventoryService.getMode().subscribe({
      next: mode => this.inventoryAccept.set(mode),
      error: () => this.messageService.add({ severity: 'warn', summary: 'Could not read inventory mode' })
    });
    this.paymentService.getMode().subscribe({
      next: mode => this.paymentAccept.set(mode),
      error: () => {}
    });
    this.paymentService.getDelay().subscribe({
      next: d => this.paymentDelay.set(d),
      error: () => {}
    });
    this.inventoryService.getDelay().subscribe({
      next: d => this.inventoryDelay.set(d),
      error: () => {}
    });
    this.paymentService.getCrash().subscribe({
      next: v => this.paymentCrash.set(v),
      error: () => {}
    });
    this.inventoryService.getCrash().subscribe({
      next: v => this.inventoryCrash.set(v),
      error: () => {}
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
    this.importResult.set(null);
    this.importError.set(null);
    this.showSkipped.set(false);
  }

  importCatalogue(): void {
    const file = this.selectedFile();
    if (!file || this.importing()) return;
    this.importing.set(true);
    this.importResult.set(null);
    this.importError.set(null);
    this.showSkipped.set(false);
    this.productService.importProducts(file).subscribe({
      next: result => {
        this.importing.set(false);
        this.importResult.set(result);
        this.cartService.clear();
      },
      error: (err) => {
        this.importing.set(false);
        const message = err?.error?.error;
        this.importError.set(message ?? 'Import failed — server error. Check logs for details.');
      }
    });
  }

  setInventoryMode(checked: boolean) {
    this.inventoryService.setMode(checked).subscribe({
      next: () => {
        this.inventoryAccept.set(checked);
        this.messageService.add({
          severity: 'success',
          summary: checked ? 'Inventory now accepting orders' : 'Inventory now rejecting orders'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory mode' })
    });
  }

  setPaymentMode(checked: boolean) {
    this.paymentService.setMode(checked).subscribe({
      next: () => {
        this.paymentAccept.set(checked);
        this.messageService.add({
          severity: 'success',
          summary: checked ? 'Payment now accepting all payments' : 'Payment now rejecting all payments'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update payment mode' })
    });
  }

  setPaymentDelay(seconds: number) {
    this.paymentService.setDelay(seconds).subscribe({
      next: () => {
        this.paymentDelay.set(seconds);
        this.messageService.add({ severity: 'success', summary: `Payment delay set to ${seconds}s` });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update payment delay' })
    });
  }

  setInventoryDelay(seconds: number) {
    this.inventoryService.setDelay(seconds).subscribe({
      next: () => {
        this.inventoryDelay.set(seconds);
        this.messageService.add({ severity: 'success', summary: `Inventory delay set to ${seconds}s` });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory delay' })
    });
  }

  setPaymentCrash(enabled: boolean) {
    this.paymentService.setCrash(enabled).subscribe({
      next: () => {
        this.paymentCrash.set(enabled);
        this.messageService.add({
          severity: enabled ? 'warn' : 'success',
          summary: enabled ? 'Payment consumer will CRASH on every message' : 'Payment consumer crash mode disabled'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update payment crash mode' })
    });
  }

  setInventoryCrash(enabled: boolean) {
    this.inventoryService.setCrash(enabled).subscribe({
      next: () => {
        this.inventoryCrash.set(enabled);
        this.messageService.add({
          severity: enabled ? 'warn' : 'success',
          summary: enabled ? 'Inventory consumer will CRASH on every message' : 'Inventory consumer crash mode disabled'
        });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed to update inventory crash mode' })
    });
  }
}