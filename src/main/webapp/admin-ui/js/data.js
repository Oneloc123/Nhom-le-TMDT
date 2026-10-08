export const demoLabel = "Dữ liệu demo · không kết nối hệ thống thật";

export const technologies = ["FDM", "SLA", "SLS", "MJF"];
export const materials = ["PLA", "PETG", "ABS", "Resin tiêu chuẩn", "Nylon PA12"];
export const surfaceQualities = ["Tiêu chuẩn", "Mịn", "Siêu mịn"];
export const categories = ["Figurine", "Đồ trang trí", "Phụ kiện", "Mô hình kiến trúc", "Dịch vụ in"];

export const users = [
  { id: "CUS-24018", name: "Nguyễn Minh Anh", email: "minhanh.nguyen@example.vn", phone: "090 312 48 26", orders: 14, totalSpent: 4280000, joined: "2026-08-21", status: "Active", initials: "MA", color: "lilac" },
  { id: "CUS-23982", name: "Trần Quốc Bảo", email: "quocbao.tran@example.vn", phone: "091 672 03 14", orders: 8, totalSpent: 2670000, joined: "2026-08-17", status: "Active", initials: "QB", color: "mint" },
  { id: "CUS-23841", name: "Lê Thanh Hà", email: "thanhha.le@example.vn", phone: "098 220 61 35", orders: 11, totalSpent: 3250000, joined: "2026-08-12", status: "Active", initials: "TH", color: "peach" },
  { id: "CUS-23690", name: "Phạm Gia Huy", email: "giahuy.pham@example.vn", phone: "093 884 15 72", orders: 3, totalSpent: 940000, joined: "2026-08-09", status: "Suspended", initials: "GH", color: "sky" },
  { id: "CUS-23576", name: "Võ Ngọc Linh", email: "ngoclinh.vo@example.vn", phone: "090 742 58 19", orders: 6, totalSpent: 1810000, joined: "2026-08-02", status: "Active", initials: "NL", color: "lemon" },
  { id: "CUS-23358", name: "Đặng Hoàng Nam", email: "hoangnam.dang@example.vn", phone: "097 113 29 80", orders: 2, totalSpent: 620000, joined: "2026-07-27", status: "Active", initials: "HN", color: "rose" },
  { id: "CUS-23127", name: "Bùi Khánh Vy", email: "khanhvy.bui@example.vn", phone: "096 540 37 11", orders: 9, totalSpent: 2930000, joined: "2026-07-19", status: "Active", initials: "KV", color: "lilac" },
  { id: "CUS-22815", name: "Phan Đức Khang", email: "duckhang.phan@example.vn", phone: "094 250 68 42", orders: 4, totalSpent: 1260000, joined: "2026-07-10", status: "Inactive", initials: "DK", color: "mint" },
  { id: "CUS-22543", name: "Hoàng Yến Nhi", email: "yennhi.hoang@example.vn", phone: "091 805 44 23", orders: 12, totalSpent: 3710000, joined: "2026-06-29", status: "Active", initials: "YN", color: "peach" },
  { id: "CUS-22102", name: "Đỗ Nhật Quang", email: "nhatquang.do@example.vn", phone: "098 419 75 06", orders: 5, totalSpent: 1540000, joined: "2026-06-20", status: "Active", initials: "NQ", color: "sky" }
];

export const sellers = [
  { id: "SEL-0082", ownerName: "Nguyễn Hoàng Phúc", name: "Mộc Lab 3D", location: "Quận 7, TP. Hồ Chí Minh", city: "TP. Hồ Chí Minh", address: "18 đường số 4, Tân Phú, Quận 7", phone: "090 811 23 45", email: "hello@moclab.example.vn", products: 28, orders: 184, revenue: 42860000, rating: 4.9, reviews: 126, deposit: 12000000, requiredDeposit: 10000000, status: "Active", joined: "2026-02-12", technology: ["FDM", "SLA"], coords: [734, 499], initials: "ML", color: "lilac" },
  { id: "SEL-0075", ownerName: "Trần Lệ Thu", name: "Xưởng Nhỏ Studio", location: "Cầu Giấy, Hà Nội", city: "Hà Nội", address: "42 ngõ 68 Cầu Giấy, Dịch Vọng", phone: "098 230 66 51", email: "xin chao@xuongnho.example.vn", products: 19, orders: 142, revenue: 36120000, rating: 4.8, reviews: 98, deposit: 8000000, requiredDeposit: 8000000, status: "Active", joined: "2026-03-04", technology: ["FDM", "SLS"], coords: [570, 169], initials: "XN", color: "mint" },
  { id: "SEL-0064", ownerName: "Lê Anh Khoa", name: "Layer & Form", location: "Hải Châu, Đà Nẵng", city: "Đà Nẵng", address: "112 Trưng Nữ Vương, Hải Châu", phone: "093 715 38 20", email: "studio@layerform.example.vn", products: 34, orders: 96, revenue: 24840000, rating: 4.7, reviews: 72, deposit: 4500000, requiredDeposit: 7000000, status: "Active", joined: "2026-04-18", technology: ["SLA", "MJF"], coords: [676, 352], initials: "LF", color: "peach" },
  { id: "SEL-0059", ownerName: "Phạm Minh Tú", name: "Góc In Sài Gòn", location: "Bình Thạnh, TP. Hồ Chí Minh", city: "TP. Hồ Chí Minh", address: "7 Nguyễn Gia Trí, Bình Thạnh", phone: "091 445 87 19", email: "hi@gocinsaigon.example.vn", products: 16, orders: 78, revenue: 18960000, rating: 4.6, reviews: 53, deposit: 9000000, requiredDeposit: 8000000, status: "Active", joined: "2026-05-08", technology: ["FDM"], coords: [726, 492], initials: "GI", color: "sky" },
  { id: "SEL-0041", ownerName: "Vũ Đức Long", name: "Layer House", location: "Nam Từ Liêm, Hà Nội", city: "Hà Nội", address: "25 phố Hàm Nghi, Mỹ Đình", phone: "097 660 31 28", email: "care@layerhouse.example.vn", products: 12, orders: 51, revenue: 12420000, rating: 4.4, reviews: 34, deposit: 3000000, requiredDeposit: 8000000, status: "Inactive", joined: "2026-05-21", technology: ["FDM", "SLA"], coords: [579, 177], initials: "LH", color: "lemon" },
  { id: "SEL-0038", ownerName: "Đỗ Thảo My", name: "Hình Khối Lab", location: "Sơn Trà, Đà Nẵng", city: "Đà Nẵng", address: "9 An Thượng 5, Ngũ Hành Sơn", phone: "090 556 02 93", email: "team@hinhkhoi.example.vn", products: 22, orders: 63, revenue: 15180000, rating: 4.8, reviews: 46, deposit: 10000000, requiredDeposit: 7000000, status: "Suspended", joined: "2026-06-11", technology: ["SLA", "SLS"], coords: [685, 358], initials: "HK", color: "rose" }
];

export const products = [
  { id: "PRD-1048", name: "Mô hình nhà phố Sài Gòn", sellerId: "SEL-0082", type: "READY-MADE", category: "Mô hình kiến trúc", price: 685000, status: "Pending Review", created: "2026-10-08", imageLabel: "Sài Gòn · 1:100", tone: "coral", description: "Mô hình mặt tiền nhà phố phong cách Sài Gòn xưa, in resin độ nét cao.", stock: 12, rating: 4.9 },
  { id: "PRD-1047", name: "Chậu cây khối Orbit", sellerId: "SEL-0075", type: "READY-MADE", category: "Đồ trang trí", price: 245000, status: "Published", created: "2026-10-07", imageLabel: "ORBIT / PLANTER", tone: "mint", description: "Chậu cây hình khối xoắn nhẹ, có lỗ thoát nước và khay hứng đồng bộ.", stock: 32, rating: 4.8 },
  { id: "PRD-1046", name: "Dịch vụ in mô hình resin", sellerId: "SEL-0064", type: "CUSTOM PRINTING SERVICE", category: "Dịch vụ in", price: 180000, status: "Pending Review", created: "2026-10-07", imageLabel: "RESIN / SLA", tone: "violet", description: "Nhận in mô hình resin theo file khách hàng, báo giá dựa trên thể tích và độ mịn.", technologies: ["SLA"], materials: ["Resin tiêu chuẩn"], colors: ["Trắng", "Xám", "Đen"], surface: ["Mịn", "Siêu mịn"], dimensions: "Tối đa 220 × 220 × 250 mm", pricing: "Từ 180.000₫ / chi tiết", examples: ["Tượng mini", "Mô hình kiến trúc"] },
  { id: "PRD-1045", name: "Móc khóa linh vật Rồng Mây", sellerId: "SEL-0082", type: "READY-MADE", category: "Phụ kiện", price: 89000, status: "Published", created: "2026-10-06", imageLabel: "RỒNG MÂY / 6 CM", tone: "blue", description: "Móc khóa linh vật rồng cách điệu, có vòng thép không gỉ.", stock: 86, rating: 4.9 },
  { id: "PRD-1044", name: "In FDM chi tiết kỹ thuật", sellerId: "SEL-0059", type: "CUSTOM PRINTING SERVICE", category: "Dịch vụ in", price: 95000, status: "Published", created: "2026-10-05", description: "Dịch vụ in chi tiết chức năng, tối ưu dung sai lắp ghép và độ bền.", technologies: ["FDM"], materials: ["PLA", "PETG", "ABS"], colors: ["Trắng", "Đen", "Xanh lá", "Cam"], surface: ["Tiêu chuẩn"], dimensions: "Tối đa 300 × 300 × 400 mm", pricing: "Từ 950₫ / gram", examples: ["Vỏ thiết bị", "Giá đỡ", "Prototype"] },
  { id: "PRD-1043", name: "Đèn ngủ địa hình Bà Nà", sellerId: "SEL-0064", type: "READY-MADE", category: "Đồ trang trí", price: 420000, status: "Rejected", created: "2026-10-03", imageLabel: "BÀ NÀ / LAMP", tone: "orange", description: "Đèn ngủ khắc địa hình Bà Nà, dùng LED USB-C.", stock: 9, rating: 4.5, rejectionReason: "Vui lòng bổ sung hình ảnh thực tế của sản phẩm." },
  { id: "PRD-1042", name: "In nylon PA12 SLS", sellerId: "SEL-0075", type: "CUSTOM PRINTING SERVICE", category: "Dịch vụ in", price: 360000, status: "Hidden", created: "2026-09-29", description: "In nylon PA12 bền, nhẹ, phù hợp khớp nối và chi tiết chuyển động.", technologies: ["SLS"], materials: ["Nylon PA12"], colors: ["Trắng tự nhiên"], surface: ["Tiêu chuẩn", "Mịn"], dimensions: "Tối đa 300 × 300 × 300 mm", pricing: "Báo giá theo file", examples: ["Khớp nối", "Vỏ máy"] },
  { id: "PRD-1041", name: "Bộ tượng cá voi mini", sellerId: "SEL-0038", type: "READY-MADE", category: "Figurine", price: 195000, status: "Draft", created: "2026-09-25", imageLabel: "OCEAN / SET 3", tone: "sky", description: "Bộ ba tượng cá voi trang trí bàn làm việc.", stock: 18, rating: 4.8 }
];

export const orders = [
  { id: "ORD-26081042", customerId: "CUS-24018", sellerId: "SEL-0082", productId: "PRD-1045", type: "READY-MADE", amount: 445000, payment: "Đã thanh toán", status: "Shipping", created: "2026-10-08T09:42:00", items: [{ name: "Móc khóa linh vật Rồng Mây", quantity: 5, unitPrice: 89000 }], breakdown: { subtotal: 445000, shipping: 25000, discount: 25000, total: 445000 }, shippingInfo: "GHN · GHN839204813VN · Dự kiến 10/10", timeline: ["Đặt hàng", "Đã xác nhận", "Đang xử lý", "Đang giao"] },
  { id: "ORD-26081038", customerId: "CUS-23982", sellerId: "SEL-0075", productId: "PRD-1047", type: "READY-MADE", amount: 515000, payment: "Đã thanh toán", status: "Processing", created: "2026-10-08T08:16:00", items: [{ name: "Chậu cây khối Orbit", quantity: 2, unitPrice: 245000 }], breakdown: { subtotal: 490000, shipping: 25000, discount: 0, total: 515000 }, shippingInfo: "Chưa bàn giao cho đơn vị vận chuyển", timeline: ["Đặt hàng", "Đã xác nhận", "Đang xử lý"] },
  { id: "ORD-26081021", customerId: "CUS-23841", sellerId: "SEL-0064", productId: "PRD-1046", type: "CUSTOM PRINT", amount: 1280000, payment: "Đã đặt cọc", status: "Printing", created: "2026-10-07T16:25:00", items: [{ name: "In mô hình kiến trúc theo file", quantity: 2, unitPrice: 640000 }], breakdown: { subtotal: 1280000, shipping: 35000, discount: 0, total: 1315000 }, shippingInfo: "Chưa bàn giao cho đơn vị vận chuyển", timeline: ["Gửi yêu cầu", "Đã báo giá", "Khách đã đồng ý", "Đang in"], custom: { fileName: "nha-pho-q3-v7.stl", dimensions: "X 120 × Y 85 × Z 160 mm", volume: "86,4 cm³", quantity: 2, scale: "1:100", technology: "SLA", material: "Resin tiêu chuẩn", color: "Xám đá", surface: "Mịn", quote: 1280000 } },
  { id: "ORD-26081003", customerId: "CUS-23576", sellerId: "SEL-0059", productId: "PRD-1044", type: "CUSTOM PRINT", amount: 865000, payment: "Đã thanh toán", status: "Customer Accepted", created: "2026-10-07T11:03:00", items: [{ name: "In vỏ cảm biến theo file", quantity: 3, unitPrice: 288333 }], breakdown: { subtotal: 865000, shipping: 30000, discount: 0, total: 895000 }, shippingInfo: "Đang chờ xưởng xác nhận lịch in", timeline: ["Gửi yêu cầu", "Đã báo giá", "Khách đã đồng ý"], custom: { fileName: "sensor-shell-r2.step", dimensions: "X 78 × Y 54 × Z 32 mm", volume: "24,8 cm³", quantity: 3, scale: "1:1", technology: "FDM", material: "PETG", color: "Đen", surface: "Tiêu chuẩn", quote: 865000 } },
  { id: "ORD-26080984", customerId: "CUS-23358", sellerId: "SEL-0082", productId: "PRD-1045", type: "READY-MADE", amount: 292000, payment: "Đã thanh toán", status: "Completed", created: "2026-10-06T14:40:00", items: [{ name: "Móc khóa linh vật Rồng Mây", quantity: 3, unitPrice: 89000 }], breakdown: { subtotal: 267000, shipping: 25000, discount: 0, total: 292000 }, shippingInfo: "Giao thành công · 08/10", timeline: ["Đặt hàng", "Đã xác nhận", "Đang xử lý", "Đang giao", "Hoàn tất"] },
  { id: "ORD-26080957", customerId: "CUS-23127", sellerId: "SEL-0064", productId: "PRD-1046", type: "CUSTOM PRINT", amount: 0, payment: "Chưa thanh toán", status: "Pending Quote", created: "2026-10-06T10:28:00", items: [{ name: "Tượng chibi theo ảnh", quantity: 1, unitPrice: 0 }], breakdown: { subtotal: 0, shipping: 35000, discount: 0, total: 0 }, shippingInfo: "Chưa xác nhận", timeline: ["Gửi yêu cầu"], custom: { fileName: "chibi-reference.obj", dimensions: "X 92 × Y 80 × Z 145 mm", volume: "—", quantity: 1, scale: "1:10", technology: "SLA", material: "Resin tiêu chuẩn", color: "Chưa chọn", surface: "Siêu mịn", quote: null } },
  { id: "ORD-26080924", customerId: "CUS-22543", sellerId: "SEL-0075", productId: "PRD-1047", type: "READY-MADE", amount: 270000, payment: "Đã hoàn tiền", status: "Refunded", created: "2026-10-05T18:12:00", items: [{ name: "Chậu cây khối Orbit", quantity: 1, unitPrice: 245000 }], breakdown: { subtotal: 245000, shipping: 25000, discount: 0, total: 270000 }, shippingInfo: "Đơn đã hủy, không giao hàng", timeline: ["Đặt hàng", "Đã xác nhận", "Đã hủy", "Đã hoàn tiền"] },
  { id: "ORD-26080889", customerId: "CUS-22102", sellerId: "SEL-0059", productId: "PRD-1044", type: "CUSTOM PRINT", amount: 640000, payment: "Đã thanh toán", status: "Completed", created: "2026-10-04T09:06:00", items: [{ name: "Giá đỡ camera theo file", quantity: 2, unitPrice: 320000 }], breakdown: { subtotal: 640000, shipping: 30000, discount: 0, total: 670000 }, shippingInfo: "Giao thành công · 07/10", timeline: ["Gửi yêu cầu", "Đã báo giá", "Khách đã đồng ý", "Đang in", "Đang giao", "Hoàn tất"], custom: { fileName: "camera-mount-v4.stl", dimensions: "X 110 × Y 64 × Z 48 mm", volume: "48,1 cm³", quantity: 2, scale: "1:1", technology: "FDM", material: "PLA", color: "Đen", surface: "Tiêu chuẩn", quote: 640000 } }
];

export const transactions = [
  { id: "TXN-890143", orderId: "ORD-26081042", customerId: "CUS-24018", sellerId: "SEL-0082", gross: 445000, commission: 44500, sellerRevenue: 400500, refund: 0, date: "2026-10-08", status: "Settled" },
  { id: "TXN-890132", orderId: "ORD-26081038", customerId: "CUS-23982", sellerId: "SEL-0075", gross: 515000, commission: 51500, sellerRevenue: 463500, refund: 0, date: "2026-10-08", status: "Settled" },
  { id: "TXN-890101", orderId: "ORD-26081021", customerId: "CUS-23841", sellerId: "SEL-0064", gross: 1280000, commission: 128000, sellerRevenue: 1152000, refund: 0, date: "2026-10-07", status: "Deposit received" },
  { id: "TXN-889978", orderId: "ORD-26080984", customerId: "CUS-23358", sellerId: "SEL-0082", gross: 292000, commission: 29200, sellerRevenue: 262800, refund: 0, date: "2026-10-06", status: "Settled" },
  { id: "TXN-889944", orderId: "ORD-26080924", customerId: "CUS-22543", sellerId: "SEL-0075", gross: 270000, commission: 27000, sellerRevenue: 243000, refund: 270000, date: "2026-10-05", status: "Refunded" },
  { id: "TXN-889901", orderId: "ORD-26080889", customerId: "CUS-22102", sellerId: "SEL-0059", gross: 670000, commission: 67000, sellerRevenue: 603000, refund: 0, date: "2026-10-04", status: "Settled" }
];

export const orderItems = orders.flatMap(order => order.items.map((item, index) => ({
  id: `${order.id}-ITM-${index + 1}`,
  orderId: order.id,
  productId: order.productId,
  name: item.name,
  quantity: item.quantity,
  unitPrice: item.unitPrice,
  lineTotal: item.quantity * item.unitPrice
})));

export const depositTransactions = [
  { id: "DEP-61028", sellerId: "SEL-0082", type: "Deposit", amount: 4000000, date: "2026-10-02", note: "Bổ sung ký quỹ quý IV" },
  { id: "DEP-61017", sellerId: "SEL-0075", type: "Adjustment", amount: 1000000, date: "2026-09-28", note: "Điều chỉnh theo quy mô xưởng" },
  { id: "DEP-60992", sellerId: "SEL-0064", type: "Refund", amount: -2500000, date: "2026-09-20", note: "Hoàn một phần tiền cọc" },
  { id: "DEP-60951", sellerId: "SEL-0059", type: "Deposit", amount: 2000000, date: "2026-09-12", note: "Nạp tiền cọc" },
  { id: "DEP-60918", sellerId: "SEL-0041", type: "Refund", amount: -1000000, date: "2026-09-03", note: "Hoàn tiền theo yêu cầu" },
  { id: "DEP-60886", sellerId: "SEL-0038", type: "Deposit", amount: 3000000, date: "2026-08-29", note: "Nạp tiền cọc" }
];

export const commissionRules = [
  { id: "COM-01", name: "Toàn marketplace", scope: "All sellers", productType: "All products", rate: 10, enabled: true },
  { id: "COM-02", name: "In theo yêu cầu", scope: "All sellers", productType: "CUSTOM PRINTING SERVICE", rate: 12, enabled: false }
];

export const reviews = [
  { id: "REV-8204", userId: "CUS-24018", sellerId: "SEL-0082", productId: "PRD-1045", rating: 5, date: "2026-10-07", text: "Móc khóa rất sắc nét, đóng gói cẩn thận." },
  { id: "REV-8178", userId: "CUS-23982", sellerId: "SEL-0075", productId: "PRD-1047", rating: 5, date: "2026-10-05", text: "Màu đẹp, chậu chắc tay và đúng mô tả." },
  { id: "REV-8091", userId: "CUS-23358", sellerId: "SEL-0064", productId: "PRD-1046", rating: 4, date: "2026-10-02", text: "Bản in sạch, xưởng trao đổi thông số rất kỹ." },
  { id: "REV-8032", userId: "CUS-22543", sellerId: "SEL-0059", productId: "PRD-1044", rating: 5, date: "2026-09-28", text: "Chi tiết lắp vừa khít, thời gian giao nhanh." }
];

export const dashboardSeries = {
  "7 days": [18, 24, 20, 32, 28, 38, 34],
  "30 days": [20, 27, 24, 36, 31, 44, 39, 53, 46, 58, 50, 66],
  "3 months": [28, 32, 39, 34, 48, 45, 54, 62, 60, 72, 68, 84],
  "6 months": [22, 31, 28, 42, 39, 51, 47, 60, 56, 69, 65, 82],
  "1 year": [18, 24, 29, 26, 35, 44, 39, 48, 56, 53, 66, 84]
};

export const sampleSettings = {
  platformName: "Print3D Studio",
  currency: "VND · Việt Nam Đồng",
  timezone: "Asia/Ho_Chi_Minh (UTC+7)",
  requiredDeposit: 8000000,
  requireProductReview: true,
  printingTechnologies: [...technologies],
  materials: [...materials],
  surfaceQualities: [...surfaceQualities]
};
