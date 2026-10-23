export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}