const BASE = '/api'

async function req<T>(method: string, path: string, body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
    credentials: 'include',
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }))
    throw new Error(err.error || res.statusText)
  }
  return res.json()
}

// ---- Types ----
export interface Product { productID: number; name: string; price: number; category: string }
export interface CafeTable { tableID: number; capacity: number; location: string }
export interface Customer { customerID: number; firstName: string; lastName: string }
export interface OrderItem {
  orderItemID: number; orderID: number; productID: number; productName: string
  quantity: number; preparedCount: number; priceAtOrder: number
}
export interface Order {
  orderID: number; customerID: number; tableID: number | null
  time: number; status: string; paymentMethod: string; isOnline: boolean
}
export interface AdminOrder extends Order { customerName: string }
export interface Ingredient {
  ingredientID: number; name: string; type: string; stock: number
  restockThreshold: number; supplierID: number
}
export interface Supplier {
  supplierID: number; name: string; contact: string; email: string
  address: string; rating: number
}
export interface RestockRequest {
  requestID: number; ingredientID: number; ingredientName: string
  supplierID: number; supplierName: string; quantityRequested: number
  status: string; requestedAt: number
}
export interface DashboardStats {
  ordersToday: number; pendingOrders: number; lowStockItems: number
  pendingRestocks: number; salesToday: number
  topProducts: { productID: number; productName: string; totalQuantitySold: number; price: number }[]
}
export interface SalesReport {
  ordersToday: number; salesToday: number; pendingOrders: number
  completedOrders: number
  topProducts: { productID: number; productName: string; totalQuantitySold: number; price: number }[]
}

// ---- Public API ----
export const api = {
  getProducts: () => req<{ products: Product[] }>('GET', '/products'),
  getTables:   () => req<{ tables: CafeTable[] }>('GET', '/tables'),

  registerCustomer: (firstName: string, lastName: string) =>
    req<Customer>('POST', '/customers', { firstName, lastName }),
  getCustomer: (id: number) => req<Customer>('GET', `/customers/${id}`),

  placeOrder: (payload: {
    customerID: number; tableID?: number | null; paymentMethod: string
    items: { productID: number; quantity: number }[]
  }) => req<{ orderID: number; total: number }>('POST', '/orders', payload),

  getOrders:  (customerId: number) =>
    req<{ orders: Order[] }>('GET', `/orders?customerId=${customerId}`),
  getOrder:   (id: number, customerId: number) =>
    req<{ order: Order; items: OrderItem[] }>('GET', `/orders/${id}?customerId=${customerId}`),

  // ---- Admin ----
  adminLogin:  (password: string) =>
    req<{ role: string }>('POST', '/admin/login', { password }),
  adminLogout: () => req('POST', '/admin/logout'),

  adminDashboard: () => req<DashboardStats>('GET', '/admin/dashboard'),
  adminSales:     () => req<SalesReport>('GET', '/admin/sales'),

  adminGetOrders: (status?: string) =>
    req<{ orders: AdminOrder[] }>('GET', `/admin/orders${status ? `?status=${status}` : ''}`),
  adminGetOrder:  (id: number) =>
    req<{ order: AdminOrder; items: OrderItem[] }>('GET', `/admin/orders/${id}`),
  adminUpdateOrderStatus: (id: number, status: string) =>
    req('POST', `/admin/orders/${id}/status`, { status }),
  adminConfirmOrder: (id: number) =>
    req('POST', `/admin/orders/${id}/confirm`),

  adminGetProducts:    () => req<{ products: Product[] }>('GET', '/admin/products'),
  adminAddProduct:     (p: Omit<Product, 'productID'>) =>
    req<{ productID: number }>('POST', '/admin/products', p),
  adminUpdateProduct:  (id: number, p: Partial<Omit<Product, 'productID'>>) =>
    req('PUT', `/admin/products/${id}`, p),
  adminDeleteProduct:  (id: number) =>
    req('DELETE', `/admin/products/${id}`),

  adminGetInventory:   () =>
    req<{ ingredients: Ingredient[]; suppliers: Supplier[] }>('GET', '/admin/inventory'),
  adminGetSuppliers:   () => req<{ suppliers: Supplier[] }>('GET', '/admin/suppliers'),
  adminRequestRestock: (ingredientID: number, supplierID: number, quantity: number) =>
    req<{ requestID: number }>('POST', '/admin/restock-requests', { ingredientID, supplierID, quantity }),
  adminGetRestockRequests: () =>
    req<{ requests: RestockRequest[] }>('GET', '/admin/restock-requests'),
}
