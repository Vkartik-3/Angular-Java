export interface Product {
  id: string;
  category: string;
  price: number;
  imageUrl: string;
  attributes: Record<string, string>;
}

export interface SkippedRecord {
  identifier: string;
  reason: string;
}

export interface ImportResult {
  imported: number;
  skipped: number;
  skippedRecords: SkippedRecord[];
}